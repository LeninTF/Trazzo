import 'zone.js';
import 'zone.js/testing';
import { getTestBed } from '@angular/core/testing';
import {
    BrowserDynamicTestingModule,
    platformBrowserDynamicTesting
} from '@angular/platform-browser-dynamic/testing';

const mockModalInstance = { show: () => {}, hide: () => {} };
(window as any).bootstrap = {
  Modal: Object.assign(
    function () { return mockModalInstance; },
    { getInstance: () => mockModalInstance }
  ),
};

getTestBed().initTestEnvironment(
    BrowserDynamicTestingModule,
    platformBrowserDynamicTesting()
);