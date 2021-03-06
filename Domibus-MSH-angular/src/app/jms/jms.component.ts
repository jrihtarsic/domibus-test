import {Component, EventEmitter, OnInit, TemplateRef, ViewChild} from '@angular/core';
import {Http, Response} from '@angular/http';
import {AlertService} from '../common/alert/alert.service';
import {MessagesRequestRO} from './ro/messages-request-ro';
import {isNullOrUndefined} from 'util';
import {MdDialog, MdDialogRef} from '@angular/material';
import {MoveDialogComponent} from './move-dialog/move-dialog.component';
import {MessageDialogComponent} from './message-dialog/message-dialog.component';
import {CancelDialogComponent} from '../common/cancel-dialog/cancel-dialog.component';
import {DirtyOperations} from '../common/dirty-operations';
import {ColumnPickerBase} from '../common/column-picker/column-picker-base';
import {RowLimiterBase} from '../common/row-limiter/row-limiter-base';
import {Observable} from 'rxjs/Observable';
import {DownloadService} from '../common/download.service';
import {AlertComponent} from '../common/alert/alert.component';
import mix from '../common/mixins/mixin.utils';
import BaseListComponent from '../common/base-list.component';
import FilterableListMixin from '../common/mixins/filterable-list.mixin';

@Component({
  selector: 'app-jms',
  templateUrl: './jms.component.html',
  styleUrls: ['./jms.component.css']
})
export class JmsComponent extends mix(BaseListComponent).with(FilterableListMixin) implements OnInit, DirtyOperations {

  columnPicker: ColumnPickerBase = new ColumnPickerBase();
  rowLimiter: RowLimiterBase = new RowLimiterBase();

  dateFormat: String = 'yyyy-MM-dd HH:mm:ssZ';

  timestampFromMaxDate: Date;
  timestampToMinDate: Date;
  timestampToMaxDate: Date;

  defaultQueueSet: EventEmitter<boolean>;
  queuesInfoGot: EventEmitter<boolean>;

  @ViewChild('rowWithDateFormatTpl') rowWithDateFormatTpl: TemplateRef<any>;
  @ViewChild('rowWithJSONTpl') rowWithJSONTpl: TemplateRef<any>;
  @ViewChild('rawTextTpl') public rawTextTpl: TemplateRef<any>;
  @ViewChild('rowActions') rowActions: TemplateRef<any>;

  queues: any[];
  orderedQueues: any[];

  currentSearchSelectedSource;

  selectedMessages: Array<any>;
  markedForDeletionMessages: Array<any>;
  loading: boolean;

  rows: Array<any>;
  request: MessagesRequestRO;

  private _selectedSource: any;
  offset: any;

  get selectedSource(): any {
    return this._selectedSource;
  }

  set selectedSource(value: any) {
    var oldVal = this._selectedSource;
    this._selectedSource = value;
    this.filter.source = value.name;
    this.defaultQueueSet.emit(oldVal);
  }

  constructor(private http: Http, private alertService: AlertService, public dialog: MdDialog) {
    super();
  }

  ngOnInit() {
    super.ngOnInit();

    this.filter = new MessagesRequestRO();

    this.offset = 0;
    this.timestampFromMaxDate = new Date();
    this.timestampToMinDate = null;
    this.timestampToMaxDate = new Date();

    this.defaultQueueSet = new EventEmitter(false);
    this.queuesInfoGot = new EventEmitter(false);

    this.queues = [];
    this.orderedQueues = [];

    this.columnPicker.allColumns = [
      {
        name: 'ID',
        prop: 'id'
      },
      {
        name: 'JMS Type',
        prop: 'type',
        width: 80
      },
      {
        cellTemplate: this.rowWithDateFormatTpl,
        name: 'Time',
        prop: 'timestamp',
        width: 80
      },
      {
        cellTemplate: this.rawTextTpl,
        name: 'Content',
        prop: 'content'
      },
      {
        cellTemplate: this.rowWithJSONTpl,
        name: 'Custom prop',
        prop: 'customProperties',
        width: 250
      },
      {
        cellTemplate: this.rowWithJSONTpl,
        name: 'JMS prop',
        prop: 'jmsproperties',
        width: 200
      },
      {
        cellTemplate: this.rowActions,
        name: 'Actions',
        width: 10,
        sortable: false
      }

    ];

    this.columnPicker.selectedColumns = this.columnPicker.allColumns.filter(col => {
      return ['ID', 'Time', 'Custom prop', 'JMS prop', 'Actions'].indexOf(col.name) != -1
    });

    // set toDate equals to now
    this.filter.toDate = new Date();
    this.filter.toDate.setHours(23, 59, 59, 999);

    this.selectedMessages = [];
    this.markedForDeletionMessages = [];
    this.loading = false;

    this.rows = [];

    this.loadDestinations();

    this.queuesInfoGot.subscribe(result => {
      this.setDefaultQueue('.*?[d|D]omibus.?DLQ');
    });

    this.defaultQueueSet.subscribe(oldVal => {
      super.trySearch().then(ok => {
        if (!ok) {
          //revert the drop-down value to the old oen
          this._selectedSource = oldVal;
        }
      });
    });
  }

  private getDestinations(): Observable<Response> {
    return this.http.get('rest/jms/destinations')
      .map(response => response.json().jmsDestinations)
      .catch((error: Response) => this.alertService.handleError('Could not load queues: ' + error));
  }

  private loadDestinations(): Observable<Response> {
    const result = this.getDestinations();
    result.subscribe(
      (destinations) => {
        this.queues = [];
        for (const key in destinations) {
          this.queues.push(destinations[key]);
        }
        this.queuesInfoGot.emit();
      }
    );

    return result;
  }

  private refreshDestinations(): Observable<Response> {
    const result = this.getDestinations();
    result.subscribe(
      (destinations) => {
        for (const key in destinations) {
          var src = destinations[key];
          const queue = this.queues.find(el => el.name === src.name);
          if (queue) {
            Object.assign(queue, src);
          }
        }
      }
    );
    return result;
  }

  private setDefaultQueue(queueName: string) {
    if (!this.queues || this.queues.length == 0) return;

    const matching = this.queues.find((el => el.name && el.name.match(queueName)));
    const toSelect = matching != null ? matching : this.queues.length[0];

    this.selectedSource = toSelect;
  }

  changePageSize(newPageSize: number) {
    super.resetFilters();
    this.rowLimiter.pageSize = newPageSize;
    this.refresh();
  }

  refresh() {
    // ugly but the grid does not feel the paging changes otherwise
    this.loading = true;
    const rows = this.rows;
    this.rows = [];

    setTimeout(() => {
      this.rows = rows;
      this.selectedMessages.length = 0;
      this.loading = false;
    }, 50);
  }

  onSelect({selected}) {
    this.selectedMessages.splice(0, this.selectedMessages.length);
    this.selectedMessages.push(...selected);
  }

  onActivate(event) {
    if ('dblclick' === event.type) {
      this.details(event.row);
    }
  }

  onTimestampFromChange(event) {
    this.timestampToMinDate = event.value;
  }

  onTimestampToChange(event) {
    this.timestampFromMaxDate = event.value;
  }

  canSearch() {
    return this.filter.source && !this.loading;
  }

  search() {
    super.setActiveFilter();
    this.doSearch();
  }

  private doSearch() {
    if (!this.filter.source) {
      this.alertService.error('Source should be set');
      return;
    }
    if (this.loading) {
      return;
    }

    this.loading = true;
    this.selectedMessages = [];
    this.markedForDeletionMessages = [];
    this.currentSearchSelectedSource = this.selectedSource;
    this.http.post('rest/jms/messages', {
      source: this.activeFilter.source,
      jmsType: this.activeFilter.jmsType,
      fromDate: this.activeFilter.fromDate,
      toDate: this.activeFilter.toDate,
      selector: this.activeFilter.selector,
    }).subscribe(
      (response: Response) => {
        this.rows = response.json().messages;
        this.offset = 0;
        this.refresh();
        this.loading = false;

        this.refreshDestinations();
      },
      error => {
        this.alertService.exception('An error occurred. In case you are using the Selector / JMS Type, please follow the rules for Selector / JMS Type according to Help Page / Admin Guide. ', error);
        this.loading = false;
      }
    );
  }

  cancel() {
    this.dialog.open(CancelDialogComponent).afterClosed().subscribe(result => {
      if (result) {
        super.resetFilters();
        this.doSearch();
      }
    });
  }

  save() {
    let messageIds = this.markedForDeletionMessages.map((message) => message.id);
    //because the user can change the source after pressing search and then select the messages and press delete
    //in this case I need to use currentSearchSelectedSource
    this.serverRemove(this.currentSearchSelectedSource.name, messageIds);
  }

  move() {
    const dialogRef: MdDialogRef<MoveDialogComponent> = this.dialog.open(MoveDialogComponent);

    if (/DLQ/.test(this.currentSearchSelectedSource.name)) {

      if (this.selectedMessages.length > 1) {
        dialogRef.componentInstance.queues.push(...this.queues);
      } else {
        for (let message of this.selectedMessages) {

          try {
            let originalQueue = message.customProperties.originalQueue;
            // EDELIVERY-2814
            let originalQueueName = originalQueue.substr(originalQueue.indexOf('!') + 1);
            if (!isNullOrUndefined(originalQueue)) {
              let queues = this.queues.filter((queue) => queue.name.indexOf(originalQueueName) != -1);
              console.debug(queues);
              if (!isNullOrUndefined(queues)) {
                dialogRef.componentInstance.queues = queues;
                dialogRef.componentInstance.selectedSource = queues[0];
              }
              if (queues.length == 1) {
                dialogRef.componentInstance.destinationsChoiceDisabled = true;
              }
              break;
            }
          } catch (e) {
            console.error(e);
          }
        }


        if (dialogRef.componentInstance.queues.length == 0) {
          console.warn('Unable to determine the original queue for the selected messages');
          dialogRef.componentInstance.queues.push(...this.queues);
        }
      }
    } else {
      dialogRef.componentInstance.queues.push(...this.queues);
    }


    dialogRef.afterClosed().subscribe(result => {
      if (!isNullOrUndefined(result) && !isNullOrUndefined(result.destination)) {
        let messageIds = this.selectedMessages.map((message) => message.id);
        this.serverMove(this.currentSearchSelectedSource.name, result.destination, messageIds);
      }
    });
  }

  moveAction(row) {
    let dialogRef: MdDialogRef<MoveDialogComponent> = this.dialog.open(MoveDialogComponent);

    if (/DLQ/.test(this.currentSearchSelectedSource.name)) {
      try {
        let originalQueue = row.customProperties.originalQueue;
        // EDELIVERY-2814
        let originalQueueName = originalQueue.substr(originalQueue.indexOf('!') + 1);
        let queues = this.queues.filter((queue) => queue.name.indexOf(originalQueueName) != -1);
        console.debug(queues);
        if (!isNullOrUndefined(queues)) {
          dialogRef.componentInstance.queues = queues;
          dialogRef.componentInstance.selectedSource = queues[0];
        }
        if (queues.length == 1) {
          dialogRef.componentInstance.destinationsChoiceDisabled = true;
        }
      } catch (e) {
        console.error(e);
      }

      if (dialogRef.componentInstance.queues.length == 0) {
        console.log(dialogRef.componentInstance.queues.length);
        dialogRef.componentInstance.queues.push(...this.queues);
      }
    } else {
      dialogRef.componentInstance.queues.push(...this.queues);
    }

    dialogRef.afterClosed().subscribe(result => {
      if (!isNullOrUndefined(result) && !isNullOrUndefined(result.destination)) {
        let messageIds = this.selectedMessages.map((message) => message.id);
        this.serverMove(this.currentSearchSelectedSource.name, result.destination, messageIds);
      }
    });
  }

  details(selectedRow: any) {
    let dialogRef: MdDialogRef<MessageDialogComponent> = this.dialog.open(MessageDialogComponent);
    dialogRef.componentInstance.message = selectedRow;
    dialogRef.componentInstance.currentSearchSelectedSource = this.currentSearchSelectedSource;
    dialogRef.afterClosed().subscribe(result => {
      //Todo:
    });
  }

  deleteAction(row) {
    this.markedForDeletionMessages.push(row);
    let newRows = this.rows.filter((element) => {
      return row !== element;
    });
    this.selectedMessages = [];
    this.rows = newRows;
  }

  delete() {
    this.markedForDeletionMessages.push(...this.selectedMessages);
    let newRows = this.rows.filter((element) => {
      return !this.selectedMessages.includes(element);
    });
    this.selectedMessages = [];
    this.rows = newRows;
  }

  serverMove(source: string, destination: string, messageIds: Array<any>) {
    console.log('serverMove');
    this.http.post('rest/jms/messages/action', {
      source: source,
      destination: destination,
      selectedMessages: messageIds,
      action: 'MOVE'
    }).subscribe(
      () => {
        this.alertService.success('The operation \'move messages\' completed successfully.');

        //refresh destinations
        this.refreshDestinations().subscribe((response: Response) => {
          this.setDefaultQueue(this.currentSearchSelectedSource.name);
        });

        //remove the selected rows
        let newRows = this.rows.filter((element) => {
          return !this.selectedMessages.includes(element);
        });
        this.selectedMessages = [];
        this.rows = newRows;
      },
      error => {
        this.alertService.exception('The operation \'move messages\' could not be completed: ', error);
      }
    )
  }

  serverRemove(source: string, messageIds: Array<any>) {
    this.http.post('rest/jms/messages/action', {
      source: source,
      selectedMessages: messageIds,
      action: 'REMOVE'
    }).subscribe(
      () => {
        this.alertService.success('The operation \'updates on message(s)\' completed successfully.');
        this.refreshDestinations();
        this.markedForDeletionMessages = [];
      },
      error => {
        this.alertService.error('The operation \'updates on message(s)\' could not be completed: ' + error);
      }
    )
  }

  getFilterPath() {
    let result = '?';
    if (!isNullOrUndefined(this.activeFilter.source)) {
      result += 'source=' + this.activeFilter.source + '&';
    }
    if (!isNullOrUndefined(this.activeFilter.jmsType)) {
      result += 'jmsType=' + this.activeFilter.jmsType + '&';
    }
    if (!isNullOrUndefined(this.activeFilter.fromDate)) {
      result += 'fromDate=' + this.activeFilter.fromDate.toISOString() + '&';
    }
    if (!isNullOrUndefined(this.activeFilter.toDate)) {
      result += 'toDate=' + this.activeFilter.toDate.toISOString() + '&';
    }
    if (!isNullOrUndefined(this.activeFilter.selector)) {
      result += 'selector=' + this.activeFilter.selector + '&';
    }
    return result;
  }

  saveAsCSV() {
    if (!this.activeFilter.source) {
      this.alertService.error('Source should be set');
      return;
    }
    if (this.rows.length > AlertComponent.MAX_COUNT_CSV) {
      this.alertService.error(AlertComponent.CSV_ERROR_MESSAGE);
      return;
    }
    super.resetFilters();
    DownloadService.downloadNative('rest/jms/csv' + this.getFilterPath());
  }

  isDirty(): boolean {
    return !isNullOrUndefined(this.markedForDeletionMessages) && this.markedForDeletionMessages.length > 0;
  }

  onPage($event) {
    this.offset = $event.offset;
    super.resetFilters();
  }

  onSort() {
    super.resetFilters();
  }
}
