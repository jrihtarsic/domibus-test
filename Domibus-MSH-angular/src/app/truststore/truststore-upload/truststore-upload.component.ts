import {Component, Inject, ViewChild} from "@angular/core";
import {MD_DIALOG_DATA, MdDialogRef} from "@angular/material";
import {TrustStoreService} from "../trustore.service";
import {AlertService} from "../../alert/alert.service";

@Component({
  selector: 'app-trustore-upload',
  templateUrl: './truststore-upload.component.html',
  styleUrls: ['./truststore-upload.component.css'],
  providers: [TrustStoreService]
})
export class TrustStoreUploadComponent {

  password: any;
  @ViewChild('fileInput')
  private fileInput;

  constructor(public dialogRef: MdDialogRef<TrustStoreUploadComponent>,
              private truststorService: TrustStoreService, private alertService: AlertService,
              @Inject(MD_DIALOG_DATA) public data: any) {
  }

  public submit() {
    let fi = this.fileInput.nativeElement;
    this.truststorService.saveTrustStore(fi.files[0], this.password).subscribe(res => {
        this.alertService.success(res.text(), false);
        this.dialogRef.close(true)
      },
      err => {
        this.alertService.error("Error updating truststore file", false);
      }
    );
  }
}
