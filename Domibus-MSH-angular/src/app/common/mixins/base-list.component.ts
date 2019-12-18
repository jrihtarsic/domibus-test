import {AlertComponent} from '../alert/alert.component';
import {AlertService} from '../alert/alert.service';
import {DownloadService} from '../download.service';
import {OnInit} from '@angular/core';
import {ColumnPickerBase} from '../column-picker/column-picker-base';
import {IBaseList} from './ibase-list';
import {instanceOfFilterableList, instanceOfModifiableList, instanceOfPageableList, instanceOfSortableList} from './type.utils';
import {PaginationType} from './ipageable-list';
import {ErrorLogResult} from '../../errorlog/errorlogresult';
import {HttpClient, HttpParams} from '@angular/common/http';

/**
 * @author Ion Perpegel
 * @since 4.1
 *
 * Base class for list components: mainly takes care of getting data from the server
 */
export interface Constructable {
  new(...args);
}

export function ConstructableDecorator(constructor: Constructable) {
}

@ConstructableDecorator
export default class BaseListComponent<T> implements IBaseList<T>, OnInit {
  public rows: T[];
  public selected: T[];
  public count: number;
  public columnPicker: ColumnPickerBase;
  public isLoading: boolean;

  constructor(private alertService: AlertService, private http: HttpClient) {
    this.columnPicker = new ColumnPickerBase();
  }

  ngOnInit(): void {
    this.rows = [];
    this.selected = [];
    this.count = 0;
    this.isLoading = false;
  }

  public get name(): string {
    return this.constructor.name;
  }

  protected get GETUrl(): string {
    return undefined;
  }

  protected createAndSetParameters(): HttpParams {
    return new HttpParams();
  }

  public async getServerData(): Promise<any> {
    let getParams = this.createAndSetParameters();
    return this.http.get<ErrorLogResult>(this.GETUrl, {params: getParams})
      .toPromise();
  }

  public async setServerResults(data: any) {
  }

  protected async onBeforeGetData(): Promise<any> {
    return true;
  }

  public async getDataAndSetResults(): Promise<any> {
    await this.onBeforeGetData();

    let data = await this.getServerData();
    this.setServerResults(data);
  }

  public async loadServerData(): Promise<any> {
    if (this.isLoading) {
      return null;
    }

    this.isLoading = true;
    this.selected.length = 0;

    if (instanceOfFilterableList(this)) {
      this.resetFilters();
    }

    let result: any;
    try {
      result = await this.getDataAndSetResults();
    } catch (error) {
      this.isLoading = false;
      this.alertService.exception(`Error loading data for '${this.name}' component:`, error);
      throw error;
    }

    this.isLoading = false;

    if (instanceOfModifiableList(this)) {
      this.isChanged = false;
      this.selected = [];
    }

    // TODO : review this. it should be removed.
    // if (instanceOfPageableList(this) && this.type == PaginationType.Client) {
    //   this.offset = 0;
    // }

    return result;
  }

  public get csvUrl(): string {
    return undefined;
  }

  public async saveAsCSV() {
    if (instanceOfModifiableList(this)) {
      await this.saveIfNeeded();
    }

    if (this.count > AlertComponent.MAX_COUNT_CSV) {
      this.alertService.error(AlertComponent.CSV_ERROR_MESSAGE);
      return;
    }

    if (instanceOfFilterableList(this)) {
      this.resetFilters();
    }

    DownloadService.downloadNative(this.csvUrl);
  }

  protected hasMethod(name: string) {
    return this[name] && this[name] instanceof Function;
  }

  public onSelect(event) {

  }

  public onActivate(event) {
    if ('dblclick' === event.type) {
      if(instanceOfModifiableList(this)) {
        this.edit(event.row);
      } else {
        this.showDetails(event.row);
      }
    }
  }

  public showDetails(row: T) {
  }

};



