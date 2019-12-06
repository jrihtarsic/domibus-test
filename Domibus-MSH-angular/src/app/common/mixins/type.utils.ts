import {IBaseList} from './ibase-list';
import {IPageableList} from './Ipageable-list';
import {IFilterableList} from './ifilterable-list';
import {IModifiableList} from './imodifiable-list';

/**
 * @author Ion Perpegel
 * @since 4.2
 *
 * A helper class that's a bit nicer when applying multiple mixins
 * */

export function instanceOfIBaseList(object: any): object is IBaseList<any> {
  return 'rows' in object && 'count' in object && 'csvUrl' in object;
}

export function instanceOfPageableList(object: any): object is IPageableList {
  return 'offset' in object && 'changePageSize' in object && 'onPageSizeChanging' in object;
}

export function instanceOfFilterableList(object: any): object is IFilterableList {
  return 'search' in object && 'trySearch' in object && 'activeFilter' in object;
}

export function instanceOfModifiableList(object: any): object is IModifiableList {
  return 'isDirty' in object && 'save' in object && 'saveIfNeeded' in object;
}
