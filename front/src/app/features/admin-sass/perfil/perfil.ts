import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { PerfilBase, DatosPersonales } from '../../../shared/perfil/perfil-base';

@Component({
  selector: 'app-perfil',
  imports: [FormsModule],
  templateUrl: './perfil.html',
  styleUrl: '../../../shared/perfil/perfil.css',
})
export class Perfil extends PerfilBase {
  override usuario: DatosPersonales = {
    nombres: 'Jose',
    apellidos: 'Alata',
    email: 'jose.alata@trazzo.com',
    telefono: '+51 999 888 777',
    dni: '71234567',
    rol: 'Super Administrador',
    rolSass: 'Super Administrador',
    fechaIngreso: '01/03/2020',
  };
}
