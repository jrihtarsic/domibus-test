/**
 * @author Ion Perpegel
 * @since 4.1
 *
 * A mixin for components that display a list of items that can be filtered
 */
import {Constructable} from './base-list.component';
import {OnInit} from '@angular/core';
import {instanceOfModifiableList, instanceOfPageableList} from './type.utils';
import {IFilterableList} from './ifilterable-list';
import {HttpParams} from '@angular/common/http';

let FilterableListMixin = (superclass: Constructable) => class extends superclass
  implements IFilterableList, OnInit {

  public filter: any;
  public activeFilter: any;

  // protected filterParams: HttpParams;

  constructor(...args) {
    super(...args);
    this.filter = {};
  }

  ngOnInit() {
    if (super.ngOnInit) {
      super.ngOnInit();
    }

    this.filter = {};
    this.activeFilter = {};
  }

  protected onBeforeGetServerData() {
    super.onBeforeGetServerData();

    let filterParams = this.GETParams;

    Object.keys(this.activeFilter).forEach((key: string) => {
      let value = this.activeFilter[key];
      if (value) {
        if(value instanceof Date) {
          filterParams = filterParams.append(key, value.getTime());
        } else {
          filterParams = filterParams.append(key, value);
        }
      }
    });

    super.GETParams = filterParams;
  }

  /**
   * The method takes the filter params set through widgets and copies them to the active params
   * active params are the ones that are used for actual filtering of data and can be different from the ones set by the user in the UI
   */
  public setActiveFilter() {
    //just in case ngOnInit wasn't called from corresponding component class
    if (!this.activeFilter) {
      this.activeFilter = {};
    }
    Object.assign(this.activeFilter, this.filter);
  }

  /**
   * The method takes the actual filter params and copies them to the UI bound params thus synchronizing the pair so what you see it is what you get
   */
  public resetFilters() {
    this.filter = {};
    Object.assign(this.filter, this.activeFilter);
  }

  /**
   * The method is supposed to be overridden in derived classes to implement actual search
   */
  public filterData() {
    this.setActiveFilter();
    return this.loadServerData();
  }

  /**
   * The method is trying to call the search if the component doesn't have unsaved changes, otherwise raises a popup to the client
   */
  public async tryFilter(): Promise<boolean> {
    const canSearch = await this.canProceedToFilter();
    if (canSearch) {
      this.setActiveFilter();
      this.filterData();
    }
    return canSearch;
  }

  private canProceedToFilter(): Promise<boolean> {
    if (instanceOfModifiableList(this) && this.isDirty()) {
      return this.dialogsService.openCancelDialog();
    }
    return Promise.resolve(true);
  }

};

export default FilterableListMixin;
