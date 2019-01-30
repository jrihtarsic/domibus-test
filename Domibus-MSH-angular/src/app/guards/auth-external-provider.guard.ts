﻿import {Injectable} from '@angular/core';
import {ActivatedRouteSnapshot, CanActivate, Router, RouterStateSnapshot} from '@angular/router';
import {SecurityService} from '../security/security.service';
import {ReplaySubject} from 'rxjs';

/**
 * It will redirect to home ('/') if the external provider = true
 */
@Injectable()
export class AuthExternalProviderGuard implements CanActivate {

  constructor (private router: Router, private securityService: SecurityService) {
  }

  canActivate (route: ActivatedRouteSnapshot, state: RouterStateSnapshot) {
    console.log('AuthExternalProviderGuard canActivate');
    const subject = new ReplaySubject();
    const isUserFromExternalAuthProvider = this.securityService.isUserFromExternalAuthProvider();

    if (isUserFromExternalAuthProvider) {
      console.log('redirect to /');
      this.router.navigate(['/']);
      subject.next(false);
    } else {
      subject.next(true);
    }

    return subject.asObservable();
  }
}
