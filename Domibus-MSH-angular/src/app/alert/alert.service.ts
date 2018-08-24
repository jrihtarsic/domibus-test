﻿import {Injectable} from '@angular/core';
import {NavigationEnd, NavigationStart, Router} from '@angular/router';
import {Observable} from 'rxjs';
import {Subject} from 'rxjs/Subject';

@Injectable()
export class AlertService {
  private subject = new Subject<any>();
  private previousRoute: string;


  //TODO move the logic in the ngInit block
  constructor (private router: Router) {
    this.previousRoute = '';
    // clear alert message on route change
    router.events.subscribe(event => {
      if (event instanceof NavigationStart) {
        if (this.isRouteChanged(event.url)) {
          console.log('Clearing alert when navigating from [' + this.previousRoute + '] to [' + event.url + ']');
          this.clearAlert();
        } else {
          console.log('Alert kept when navigating from [' + this.previousRoute + '] to [' + event.url + ']');
        }
      } else if (event instanceof NavigationEnd) {
        let navigationEnd: NavigationEnd = event;
        this.previousRoute = navigationEnd.url;
      }
    });
  }

  getPath (url: string): string {
    var parser = document.createElement('a');
    parser.href = url;
    return parser.pathname;
  }

  isRouteChanged (currentRoute: string): boolean {
    let result = false;
    const previousRoutePath = this.getPath(this.previousRoute);
    const currentRoutePath = this.getPath(currentRoute);
    if (previousRoutePath !== currentRoutePath) {
      result = true;
    }
    return result;
  }

  clearAlert (): void {
    this.subject.next();
  }

  success (message: string, keepAfterNavigationChange = false) {
    this.subject.next({type: 'success', text: message});
  }

  error (message: string, keepAfterNavigationChange = false, fadeTime: number = 0) {
    this.subject.next({type: 'error', text: message});
    if (fadeTime) {
      setTimeout(() => this.clearAlert(), fadeTime);
    }
  }

  exception (message: string, error: any, keepAfterNavigationChange = false, fadeTime: number = 0) {
    let errMsg = error.message;
    if (!errMsg) {
      try {
        if (error.headers && error.headers.get('content-type') !== 'text/html;charset=utf-8') {
          errMsg = (error.json ? error.json().message || error.json() || error : error);
        } else {
          errMsg = error._body ? error._body.match(/<h1>(.+)<\/h1>/)[1] : error;
        }
      } catch (e) {
      }
    }
    this.error(message + ' \n' + (errMsg || ''), keepAfterNavigationChange, fadeTime);
  }

  getMessage (): Observable<any> {
    return this.subject.asObservable();
  }
}
