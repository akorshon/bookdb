import { Component, OnInit, signal } from '@angular/core';
import { AdminService } from '../../../core/admin.service';
import { UserSummary } from '../../../core/models';

@Component({
  selector: 'app-user-management',
  standalone: true,
  imports: [],
  templateUrl: './user-management.component.html',
})
export class UserManagementComponent implements OnInit {
  users = signal<UserSummary[]>([]);
  loading = signal(true);

  constructor(private readonly adminService: AdminService) {}

  ngOnInit(): void {
    this.load();
  }

  toggleEnabled(user: UserSummary): void {
    const req = user.enabled ? this.adminService.disable(user.id) : this.adminService.enable(user.id);
    req.subscribe(() => this.load());
  }

  promote(user: UserSummary): void {
    this.adminService.promote(user.id).subscribe(() => this.load());
  }

  private load(): void {
    this.loading.set(true);
    this.adminService.listUsers(0, 100).subscribe({
      next: (res) => {
        this.users.set(res.content);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }
}
