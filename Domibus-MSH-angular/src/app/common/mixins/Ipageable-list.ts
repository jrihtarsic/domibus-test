import {RowLimiterBase} from '../row-limiter/row-limiter-base';

/**
 * @author Ion Perpegel
 * @since 4.2
 *
 * An interface for the list with pagination ( client or server)
 * */
export interface IPageableList {
  type: PaginationType;
  offset: number;
  rowLimiter: RowLimiterBase;
  // isLoading: boolean;

  onPageSizeChanging: CallbackFunction;

  changePageSize(newPageLimit: number);

  resetPage();

  loadPage(offset: number);

  page();

  // doLoadPage(): Promise<any>;
}

type CallbackFunction = (newPageLimit: number) => Promise<boolean>;

export enum PaginationType {
  Server,
  Client,
}
