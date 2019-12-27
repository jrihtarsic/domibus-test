import {Component, Inject} from '@angular/core';
import {NgControl, NgForm} from '@angular/forms';
import {MAT_DIALOG_DATA, MatDialogRef} from '@angular/material';
import {UserValidatorService} from '../../user/uservalidator.service';
import {PluginUserRO} from '../pluginuser';
import {PluginUserService} from '../pluginuser.service';
import {UserState} from '../../user/user';

const NEW_MODE = 'New PluginUser';
const EDIT_MODE = 'Plugin User Edit';

@Component({
  selector: 'editcertificatepluginuser-form',
  templateUrl: './editcertificatepluginuser-form.component.html',
  providers: [UserValidatorService]
})
export class EditcertificatepluginuserFormComponent {

  existingRoles = [];
  editMode: boolean;
  formTitle: string;
  user: PluginUserRO;

  public certificateIdPattern = PluginUserService.certificateIdPattern;
  public certificateIdMessage = PluginUserService.certificateIdMessage;
  public originalUserPattern = PluginUserService.originalUserPattern;
  public originalUserMessage = PluginUserService.originalUserMessage;

  constructor(public dialogRef: MatDialogRef<EditcertificatepluginuserFormComponent>,
              @Inject(MAT_DIALOG_DATA) public data: any) {

    this.existingRoles = data.userroles;
    this.user = data.user;
    this.editMode = this.user.status !== UserState[UserState.NEW];

    this.formTitle = this.editMode ? EDIT_MODE : NEW_MODE;
  }

  submitForm(userForm: NgForm) {
    if (userForm.invalid) {
      return;
    }
    this.dialogRef.close(true);
  }

  shouldShowErrors(field: NgControl | NgForm): boolean {
    return (field.touched || field.dirty) && !!field.errors;
  }

  isFormDisabled(form: NgForm) {
    return form.invalid || !form.dirty;
  }
}
