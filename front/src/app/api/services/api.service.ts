import { Injectable, inject } from '@angular/core';
import { AuthService } from './auth.service';
import { UsersService } from './users.service';
import { IncidentsService } from './incidents.service';
import { HorariosService } from './horarios.service';
import { CorehrService } from './corehr.service';
import { OrgService } from './org.service';
import { AuditService } from './audit.service';
import { ReportsService } from './reports.service';
import { SaasService } from './saas.service';
import { RequestsService } from './requests.service';
import { RolesService } from './roles.service';
import { TenantsService } from './tenants.service';
import { tenantUserToPersonal } from './helpers';

@Injectable({ providedIn: 'root' })
export class ApiService {
  readonly auth = inject(AuthService);
  readonly users = inject(UsersService);
  readonly incidents = inject(IncidentsService);
  readonly horarios = inject(HorariosService);
  readonly corehr = inject(CorehrService);
  readonly org = inject(OrgService);
  readonly audit = inject(AuditService);
  readonly reports = inject(ReportsService);
  readonly saas = inject(SaasService);
  readonly requests = inject(RequestsService);
  readonly roles = inject(RolesService);
  readonly tenants = inject(TenantsService);

  /** @deprecated Use `api.auth.login()` */
  login = this.auth.login.bind(this.auth);
  /** @deprecated Use `api.users.list()` */
  listTenantUsers = this.users.list.bind(this.users);
  /** @deprecated Use `api.users.get()` */
  getTenantUser = this.users.get.bind(this.users);
  /** @deprecated Use `api.users.create()` */
  createTenantUser = this.users.create.bind(this.users);
  /** @deprecated Use `api.users.update()` */
  updateTenantUser = this.users.update.bind(this.users);
  /** @deprecated Use `api.users.patch()` */
  patchTenantUser = this.users.patch.bind(this.users);
  /** @deprecated Use `api.users.delete()` */
  deleteTenantUser = this.users.delete.bind(this.users);
  /** @deprecated Use `api.users.assignRole()` */
  assignRole = this.users.assignRole.bind(this.users);
  /** @deprecated Use `api.users.changePassword()` */
  changePassword = this.users.changePassword.bind(this.users);
  /** @deprecated Use `api.users.getMe()` */
  getMe = this.users.getMe.bind(this.users);
  /** @deprecated Use `api.users.patchMe()` */
  patchMe = this.users.patchMe.bind(this.users);
  /** @deprecated Use `api.users.listMasters()` */
  listMasterUsers = this.users.listMasters.bind(this.users);
  /** @deprecated Use `api.users.getMaster()` */
  getMasterUser = this.users.getMaster.bind(this.users);
  /** @deprecated Use `api.users.getMasterMe()` */
  getMasterMe = this.users.getMasterMe.bind(this.users);
  /** @deprecated Use `api.incidents.listTypes()` */
  listIncidentTypes = this.incidents.listTypes.bind(this.incidents);
  /** @deprecated Use `api.incidents.createType()` */
  createIncidentType = this.incidents.createType.bind(this.incidents);
  /** @deprecated Use `api.incidents.patchType()` */
  patchIncidentType = this.incidents.patchType.bind(this.incidents);
  /** @deprecated Use `api.incidents.list()` */
  listIncidents = this.incidents.list.bind(this.incidents);
  /** @deprecated Use `api.incidents.create()` */
  createIncident = this.incidents.create.bind(this.incidents);
  /** @deprecated Use `api.incidents.get()` */
  getIncident = this.incidents.get.bind(this.incidents);
  /** @deprecated Use `api.incidents.patch()` */
  patchIncident = this.incidents.patch.bind(this.incidents);
  /** @deprecated Use `api.incidents.changeState()` */
  changeIncidentState = this.incidents.changeState.bind(this.incidents);
  /** @deprecated Use `api.incidents.listEvidence()` */
  listEvidence = this.incidents.listEvidence.bind(this.incidents);
  /** @deprecated Use `api.incidents.createEvidence()` */
  createEvidence = this.incidents.createEvidence.bind(this.incidents);
  /** @deprecated Use `api.incidents.deleteEvidence()` */
  deleteEvidence = this.incidents.deleteEvidence.bind(this.incidents);
  /** @deprecated Use `api.horarios.listShifts()` */
  listShifts = this.horarios.listShifts.bind(this.horarios);
  /** @deprecated Use `api.horarios.createShift()` */
  createShift = this.horarios.createShift.bind(this.horarios);
  /** @deprecated Use `api.horarios.getShift()` */
  getShift = this.horarios.getShift.bind(this.horarios);
  /** @deprecated Use `api.horarios.patchShift()` */
  patchShift = this.horarios.patchShift.bind(this.horarios);
  /** @deprecated Use `api.horarios.deleteShift()` */
  deleteShift = this.horarios.deleteShift.bind(this.horarios);
  /** @deprecated Use `api.horarios.listSchedules()` */
  listSchedules = this.horarios.listSchedules.bind(this.horarios);
  /** @deprecated Use `api.horarios.createSchedule()` */
  createSchedule = this.horarios.createSchedule.bind(this.horarios);
  /** @deprecated Use `api.horarios.getSchedule()` */
  getSchedule = this.horarios.getSchedule.bind(this.horarios);
  /** @deprecated Use `api.horarios.patchSchedule()` */
  patchSchedule = this.horarios.patchSchedule.bind(this.horarios);
  /** @deprecated Use `api.horarios.deleteSchedule()` */
  deleteSchedule = this.horarios.deleteSchedule.bind(this.horarios);
  /** @deprecated Use `api.horarios.listTolerancias()` */
  listTolerancias = this.horarios.listTolerancias.bind(this.horarios);
  /** @deprecated Use `api.horarios.createTolerancia()` */
  createTolerancia = this.horarios.createTolerancia.bind(this.horarios);
  /** @deprecated Use `api.horarios.patchTolerancia()` */
  patchTolerancia = this.horarios.patchTolerancia.bind(this.horarios);
  /** @deprecated Use `api.horarios.deleteTolerancia()` */
  deleteTolerancia = this.horarios.deleteTolerancia.bind(this.horarios);
  /** @deprecated Use `api.horarios.listUserSchedules()` */
  listUserSchedules = this.horarios.listUserSchedules.bind(this.horarios);
  /** @deprecated Use `api.horarios.createUserSchedule()` */
  createUserSchedule = this.horarios.createUserSchedule.bind(this.horarios);
  /** @deprecated Use `api.horarios.deleteUserSchedule()` */
  deleteUserSchedule = this.horarios.deleteUserSchedule.bind(this.horarios);
  /** @deprecated Use `api.corehr.listDevices()` */
  listDevices = this.corehr.listDevices.bind(this.corehr);
  /** @deprecated Use `api.corehr.createDevice()` */
  createDevice = this.corehr.createDevice.bind(this.corehr);
  /** @deprecated Use `api.corehr.getDevice()` */
  getDevice = this.corehr.getDevice.bind(this.corehr);
  /** @deprecated Use `api.corehr.patchDevice()` */
  patchDevice = this.corehr.patchDevice.bind(this.corehr);
  /** @deprecated Use `api.corehr.deleteDevice()` */
  deleteDevice = this.corehr.deleteDevice.bind(this.corehr);
  /** @deprecated Use `api.corehr.listBiometria()` */
  listBiometria = this.corehr.listBiometria.bind(this.corehr);
  /** @deprecated Use `api.corehr.initEnroll()` */
  initEnroll = this.corehr.initEnroll.bind(this.corehr);
  /** @deprecated Use `api.corehr.listAttendance()` */
  listAttendance = this.corehr.listAttendance.bind(this.corehr);
  /** @deprecated Use `api.corehr.getAttendance()` */
  getAttendance = this.corehr.getAttendance.bind(this.corehr);
  /** @deprecated Use `api.corehr.patchAttendance()` */
  patchAttendance = this.corehr.patchAttendance.bind(this.corehr);
  /** @deprecated Use `api.corehr.listNonWorkingDays()` */
  listNonWorkingDays = this.corehr.listNonWorkingDays.bind(this.corehr);
  /** @deprecated Use `api.corehr.createNonWorkingDay()` */
  createNonWorkingDay = this.corehr.createNonWorkingDay.bind(this.corehr);
  /** @deprecated Use `api.corehr.patchNonWorkingDay()` */
  patchNonWorkingDay = this.corehr.patchNonWorkingDay.bind(this.corehr);
  /** @deprecated Use `api.corehr.deleteNonWorkingDay()` */
  deleteNonWorkingDay = this.corehr.deleteNonWorkingDay.bind(this.corehr);
  /** @deprecated Use `api.corehr.listTenantContacts()` */
  listTenantContacts = this.corehr.listTenantContacts.bind(this.corehr);
  /** @deprecated Use `api.corehr.createTenantContact()` */
  createTenantContact = this.corehr.createTenantContact.bind(this.corehr);
  /** @deprecated Use `api.corehr.deleteTenantContact()` */
  deleteTenantContact = this.corehr.deleteTenantContact.bind(this.corehr);
  /** @deprecated Use `api.corehr.listUserDepartments()` */
  listUserDepartments = this.corehr.listUserDepartments.bind(this.corehr);
  /** @deprecated Use `api.corehr.createUserDepartment()` */
  createUserDepartment = this.corehr.createUserDepartment.bind(this.corehr);
  /** @deprecated Use `api.corehr.patchUserDepartment()` */
  patchUserDepartment = this.corehr.patchUserDepartment.bind(this.corehr);
  /** @deprecated Use `api.corehr.marcar()` */
  marcar = this.corehr.marcar.bind(this.corehr);
  /** @deprecated Use `api.corehr.syncAttendance()` */
  syncAttendance = this.corehr.syncAttendance.bind(this.corehr);
  /** @deprecated Use `api.auth.getPublicKey()` */
  getPublicKey = this.auth.getPublicKey.bind(this.auth);

  /** @deprecated Use imported `tenantUserToPersonal()` from helpers */
  static readonly tenantUserToPersonal = tenantUserToPersonal;
}
