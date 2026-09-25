import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

@Component({
  selector: 'app-request-confirmation',
  templateUrl: './request-confirmation.component.html',
  styleUrls: ['./request-confirmation.component.scss']
})
export class RequestConfirmationComponent implements OnInit {
  reference: string | null = null;

  constructor(private route: ActivatedRoute) { }

  ngOnInit(): void {
    this.reference = this.route.snapshot.queryParamMap.get('reference');
  }
}