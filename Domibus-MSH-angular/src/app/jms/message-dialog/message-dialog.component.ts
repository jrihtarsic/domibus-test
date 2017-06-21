import {Component, OnInit} from '@angular/core';
import {MdDialogRef} from "@angular/material";

@Component({
  selector: 'app-message-dialog',
  templateUrl: './message-dialog.component.html',
  styleUrls: ['./message-dialog.component.css']
})
export class MessageDialogComponent implements OnInit {

  message: any;
  currentSearchSelectedSource: any;

  constructor(public dialogRef: MdDialogRef<MessageDialogComponent>) {
  }

  ngOnInit() {
  }

}
