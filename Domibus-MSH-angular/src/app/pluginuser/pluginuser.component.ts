import {ChangeDetectorRef, Component, OnInit, TemplateRef, ViewChild} from '@angular/core';
import {ColumnPickerBase} from 'app/common/column-picker/column-picker-base';
import {AlertService} from '../common/alert/alert.service';
import {PluginUserSearchCriteria, PluginUserService} from './pluginuser.service';
import {PluginUserRO} from './pluginuser';
import {DirtyOperations} from 'app/common/dirty-operations';
import {MatDialog} from '@angular/material';
import {EditbasicpluginuserFormComponent} from './editpluginuser-form/editbasicpluginuser-form.component';
import {EditcertificatepluginuserFormComponent} from './editpluginuser-form/editcertificatepluginuser-form.component';
import {UserService} from '../user/user.service';
import {UserState} from '../user/user';
import mix from '../common/mixins/mixin.utils';
import BaseListComponent from '../common/mixins/base-list.component';
import FilterableListMixin from '../common/mixins/filterable-list.mixin';
import {DialogsService} from '../common/dialogs/dialogs.service';
import ModifiableListMixin from '../common/mixins/modifiable-list.mixin';
import {ClientPageableListMixin} from '../common/mixins/pageable-list.mixin';

@Component({
  templateUrl: './pluginuser.component.html',
  styleUrls: ['./pluginuser.component.css'],
  providers: [PluginUserService, UserService]
})
export class PluginUserComponent extends mix(BaseListComponent)
  .with(FilterableListMixin, ClientPageableListMixin, ModifiableListMixin)
  implements OnInit, DirtyOperations {

  @ViewChild('activeTpl', {static: false}) activeTpl: TemplateRef<any>;
  @ViewChild('rowActions', {static: false}) rowActions: TemplateRef<any>;

  columnPickerBasic: ColumnPickerBase = new ColumnPickerBase();
  columnPickerCert: ColumnPickerBase = new ColumnPickerBase();

  authenticationTypes: string[] = ['BASIC', 'CERTIFICATE'];
  filter: PluginUserSearchCriteria;
  columnPicker: ColumnPickerBase = new ColumnPickerBase();

  userRoles: Array<String>;

  constructor(private alertService: AlertService, private pluginUserService: PluginUserService, public dialog: MatDialog,
              private dialogsService: DialogsService, private changeDetector: ChangeDetectorRef) {
    super();
  }

  ngOnInit() {
    super.ngOnInit();

    this.filter = {authType: 'BASIC', authRole: '', userName: '', originalUser: ''};

    this.userRoles = [];

    this.getUserRoles();

    this.filterData();
  }

  public get name(): string {
    return 'Plugin Users';
  }

  ngAfterViewInit() {
    this.initColumns();
  }

  ngAfterViewChecked() {
    this.changeDetector.detectChanges();
  }

  get displayedUsers(): PluginUserRO[] {
    return this.rows.filter(el => el.status !== UserState[UserState.REMOVED]);
  }

  private initColumns() {
    this.columnPickerBasic.allColumns = [
      {name: 'User Name', prop: 'userName', width: 20},
      {name: 'Password', prop: 'hiddenPassword', width: 20, sortable: false},
      {name: 'Role', prop: 'authRoles', width: 10},
      {name: 'Active', prop: 'active', cellTemplate: this.activeTpl, width: 25},
      {name: 'Original User', prop: 'originalUser', width: 240},
      {
        cellTemplate: this.rowActions,
        name: 'Actions',
        width: 60,
        canAutoResize: true,
        sortable: false
      }
    ];
    this.columnPickerCert.allColumns = [
      {name: 'Certificate Id', prop: 'certificateId', width: 240},
      {name: 'Role', prop: 'authRoles', width: 10},
      {name: 'Original User', prop: 'originalUser', width: 240},
      {
        cellTemplate: this.rowActions,
        name: 'Actions',
        width: 60,
        canAutoResize: true,
        sortable: false
      }
    ];

    this.columnPickerBasic.selectedColumns = this.columnPickerBasic.allColumns.filter(col => true);
    this.columnPickerCert.selectedColumns = this.columnPickerCert.allColumns.filter(col => true);

    this.setColumnPicker();
  }

  setColumnPicker() {
    this.columnPicker = this.filter.authType === 'CERTIFICATE' ? this.columnPickerCert : this.columnPickerBasic;
  }

  changeAuthType(x) {
    this.clearSearchParams();

    super.tryFilter();
  }

  clearSearchParams() {
    this.filter.authRole = null;
    this.filter.originalUser = null;
    this.filter.userName = null;
  }

  async doGetData() {
    return this.pluginUserService.getUsers(this.activeFilter).toPromise()
      .then(result => {
        super.rows = result.entries;
        super.count = result.entries.length;

        this.setColumnPicker();
      });
  }

  inBasicMode(): boolean {
    return this.filter.authType === 'BASIC';
  }

  inCertificateMode(): boolean {
    return this.filter.authType === 'CERTIFICATE';
  }

  isDirty(): boolean {
    return this.isChanged;
  }

  async getUserRoles() {
    const result = await this.pluginUserService.getUserRoles().toPromise();
    this.userRoles = result;
  }

  onActivate(event) {
    if ('dblclick' === event.type) {
      this.edit(event.row);
    }
  }

  async add() {
    const newItem = this.pluginUserService.createNew();
    newItem.authenticationType = this.filter.authType;
    this.rows.push(newItem);
    super.count = this.count + 1;

    this.selected.length = 0;
    this.selected.push(newItem);

    this.setIsDirty();

    const ok = await this.openItemInEditForm(newItem, false);
    if (!ok) {
      this.rows.pop();
      super.count = this.count - 1;
      super.selected = [];
      this.setIsDirty();
    }
  }

  canEdit() {
    return this.selected.length === 1;
  }

  async edit(row?: PluginUserRO) {
    row = row || this.selected[0];
    const rowCopy = Object.assign({}, row);

    const ok = await this.openItemInEditForm(rowCopy, true);
    if (ok) {
      if (JSON.stringify(row) !== JSON.stringify(rowCopy)) { // the object changed
        Object.assign(row, rowCopy);
        if (row.status === UserState[UserState.PERSISTED]) {
          row.status = UserState[UserState.UPDATED];
          this.setIsDirty();
        }
      }
    }
  }

  private async openItemInEditForm(rowCopy: PluginUserRO, edit = true) {
    const editForm = this.inBasicMode() ? EditbasicpluginuserFormComponent : EditcertificatepluginuserFormComponent;
    const ok = await this.dialog.open(editForm, {
      data: {
        edit: edit,
        user: rowCopy,
        userroles: this.userRoles,
      }
    }).afterClosed().toPromise();
    return ok;
  }

  canSave() {
    return this.isDirty();
  }

  async doSave(): Promise<any> {
    return this.pluginUserService.saveUsers(this.rows).then(() => this.filterData());
  }

  setIsDirty() {
    super.isChanged = this.rows.filter(el => el.status !== UserState[UserState.PERSISTED]).length > 0;
  }

  canCancel() {
    return this.isDirty();
  }

  delete(row?: any) {
    const itemToDelete = row || this.selected[0];
    if (itemToDelete.status === UserState[UserState.NEW]) {
      this.rows.splice(this.rows.indexOf(itemToDelete), 1);
    } else {
      itemToDelete.status = UserState[UserState.REMOVED];
    }
    this.setIsDirty();
    this.selected.length = 0;
  }

  public get csvUrl(): string {
    return PluginUserService.CSV_URL + '?' + this.pluginUserService.createFilterParams(this.filter).toString();
  }

  onSort() {
    super.resetFilters();
  }
}
