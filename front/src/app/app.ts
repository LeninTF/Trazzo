import { Component, inject } from '@angular/core';
import { Router, RouterOutlet } from '@angular/router';
import { Header } from './components/header/header';
import { Sidebar } from './components/sidebar/sidebar';
import { RoleService } from './services/role.service';

@Component({
  selector: 'app-root',
  imports: [Header, RouterOutlet, Sidebar],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App {
  protected readonly router = inject(Router);
  protected readonly roleService = inject(RoleService);
}
