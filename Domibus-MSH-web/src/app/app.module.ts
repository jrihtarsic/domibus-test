import { BrowserModule } from '@angular/platform-browser';
import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';
import {HttpModule, Http, XHRBackend, RequestOptions} from '@angular/http';
import { MaterialModule } from '@angular/material';
import 'hammerjs';

import { NgxDatatableModule } from '@swimlane/ngx-datatable';
import { Md2Module }  from 'md2';

import {AppComponent} from './app.component';
import {LoginComponent} from "./login/login.component";
import {HomeComponent} from "./home/home.component";
import {AdminComponent} from "./admin/admin.component";
import {AuthenticatedGuard} from "./guards/authenticated.guard";
import {AuthorizedGuard} from "./guards/authorized.guard";
import {routing} from "./app.routes";
import {IsAuthorized} from "./security/is-authorized.directive";
import {ExtendedHttpClient} from "./http/extended-http-client";
import {HttpEventService} from "./http/http.event.service";
import {SecurityService} from "./security/security.service";
import {AlertComponent} from "./alert/alert.component";
import {AlertService} from "./alert/alert.service";
import {ErrorLogComponent} from "./errorlog/errorlog.component";
import {FooterComponent} from "./footer/footer.component";
import {DomibusInfoService} from "./appinfo/domibusinfo.service";

export function extendedHttpClientFactory (xhrBackend: XHRBackend, requestOptions: RequestOptions, httpEventService: HttpEventService) {
  return new ExtendedHttpClient(xhrBackend, requestOptions, httpEventService);
}

@NgModule({
  declarations: [
    AppComponent,
    LoginComponent,
    HomeComponent,
    ErrorLogComponent,
    AlertComponent,
    FooterComponent,
    AdminComponent,
    IsAuthorized
  ],
  entryComponents: [
    AppComponent
  ],
  imports: [
    BrowserModule,
    FormsModule,
    HttpModule,
    NgxDatatableModule,

    MaterialModule.forRoot(),
    Md2Module.forRoot(),
    routing
  ],
  providers: [
    AuthenticatedGuard,
    AuthorizedGuard,
    HttpEventService,
    SecurityService,
    DomibusInfoService,
    AlertService,
    {
      provide: Http,
      useFactory: extendedHttpClientFactory,
      deps: [XHRBackend, RequestOptions, HttpEventService],
      multi: false
    }
  ],
  bootstrap: [AppComponent]
})
export class AppModule { }
