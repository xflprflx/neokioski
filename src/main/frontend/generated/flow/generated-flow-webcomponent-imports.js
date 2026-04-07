import { injectGlobalWebcomponentCss } from 'Frontend/generated/jar-resources/theme-util.js';

import { injectGlobalCss } from 'Frontend/generated/jar-resources/theme-util.js';

import $cssFromFile_0 from '@vaadin/vaadin-lumo-styles/lumo.css?inline';

injectGlobalWebcomponentCss($cssFromFile_0.toString());
import '@vaadin/vertical-layout/src/vaadin-vertical-layout.js';
import '@vaadin/app-layout/src/vaadin-app-layout.js';
import '@vaadin/button/src/vaadin-button.js';
import '@vaadin/tooltip/src/vaadin-tooltip.js';
import 'Frontend/generated/jar-resources/disableOnClickFunctions.js';
import '@vaadin/horizontal-layout/src/vaadin-horizontal-layout.js';
import '@vaadin/app-layout/src/vaadin-drawer-toggle.js';
import '@vaadin/scroller/src/vaadin-scroller.js';
import '@vaadin/side-nav/src/vaadin-side-nav.js';
import '@vaadin/side-nav/src/vaadin-side-nav-item.js';
import '@vaadin/icons/vaadin-iconset.js';
import '@vaadin/icon/src/vaadin-icon.js';
import '@vaadin/message-input/src/vaadin-message-input.js';
import '@vaadin/list-box/src/vaadin-list-box.js';
import 'Frontend/generated/jar-resources/flow-component-renderer.js';
import 'Frontend/generated/jar-resources/flow-component-directive.js';
import 'lit';
import 'Frontend/generated/jar-resources/lit-renderer.ts';
import 'lit/directives/live.js';
import '@vaadin/item/src/vaadin-item.js';
import '@vaadin/split-layout/src/vaadin-split-layout.js';
import '@vaadin/combo-box/src/vaadin-combo-box.js';
import 'Frontend/generated/jar-resources/comboBoxConnector.js';
import '@vaadin/component-base/src/debounce.js';
import '@vaadin/component-base/src/async.js';
import '@vaadin/combo-box/src/vaadin-combo-box-placeholder.js';
import '@vaadin/multi-select-combo-box/src/vaadin-multi-select-combo-box.js';
import '@vaadin/date-picker/src/vaadin-date-picker.js';
import 'Frontend/generated/jar-resources/datepickerConnector.js';
import 'date-fns/parse';
import '@vaadin/date-picker/src/vaadin-date-picker-helper.js';
import '@vaadin/checkbox/src/vaadin-checkbox.js';
import '@vaadin/avatar/src/vaadin-avatar.js';
import '@vaadin/context-menu/src/vaadin-context-menu.js';
import 'Frontend/generated/jar-resources/contextMenuConnector.js';
import 'Frontend/generated/jar-resources/contextMenuTargetConnector.js';
import '@vaadin/component-base/src/gestures.js';
import '@vaadin/text-field/src/vaadin-text-field.js';
import '@vaadin/text-area/src/vaadin-text-area.js';
import '@vaadin/time-picker/src/vaadin-time-picker.js';
import 'Frontend/generated/jar-resources/vaadin-time-picker/timepickerConnector.js';
import 'Frontend/generated/jar-resources/vaadin-time-picker/helpers.js';
import '@vaadin/time-picker/src/vaadin-time-picker-helper.js';
import '@vaadin/number-field/src/vaadin-number-field.js';
import '@vaadin/checkbox-group/src/vaadin-checkbox-group.js';
import '@vaadin/radio-group/src/vaadin-radio-group.js';
import '@vaadin/radio-group/src/vaadin-radio-button.js';
import '@vaadin/dialog/src/vaadin-dialog.js';
import '@vaadin/notification/src/vaadin-notification.js';
import '@vaadin/grid/src/vaadin-grid.js';
import '@vaadin/grid/src/vaadin-grid-column.js';
import '@vaadin/grid/src/vaadin-grid-sorter.js';
import 'Frontend/generated/jar-resources/gridConnector.ts';
import '@vaadin/grid/src/vaadin-grid-active-item-mixin.js';
import 'Frontend/generated/jar-resources/vaadin-grid-flow-selection-column.js';
import '@vaadin/grid/src/vaadin-grid-column-group.js';
import '@vaadin/form-layout/src/vaadin-form-layout.js';
import '@vaadin/form-layout/src/vaadin-form-item.js';
import '@vaadin/form-layout/src/vaadin-form-row.js';
import '@vaadin/login/src/vaadin-login-form.js';
import '@vaadin/common-frontend/ConnectionIndicator.js';
import 'Frontend/generated/jar-resources/ReactRouterOutletElement.tsx';
import 'react-router';
import 'react';
const loadOnDemand = (key) => {
  const pending = [];
  if (key === '3d8665e2b55049dae8cadba00a13b6ea393bb858e4e2e6db1feb7a6dfaa548af') {
    pending.push(import('./chunks/chunk-1c11110a506bf0c73a107aa3d3ab83d06d30ffca723520182954187926a6eba7.js'));
  }
  if (key === '279ad58b88a70210be7eb02c4c5ee38c44fe1907f1e3aee52e66c4617918d8d9') {
    pending.push(import('./chunks/chunk-d043a4f9deb3834ed15295c2ab7ef30cc8ca1f40feac86ef54a2fa3d26d48b13.js'));
  }
  if (key === '8a124de4b11b9401531acd5d7a279465e217a43df68f294e46d3784a18a156e4') {
    pending.push(import('./chunks/chunk-1c11110a506bf0c73a107aa3d3ab83d06d30ffca723520182954187926a6eba7.js'));
  }
  if (key === 'b0f89507dc6e049a3b319ff948105db657bd6665bb5829e7ca12ff0571d936b5') {
    pending.push(import('./chunks/chunk-f92fbbf0e3bd10e059faf4311a1d6dc97f89e2c1b511560531ba15d48ba7f1f6.js'));
  }
  if (key === 'cdc9b6104f715b9e4f90517b753d6d040e65599339ff62955cc862a70b74ddbf') {
    pending.push(import('./chunks/chunk-d043a4f9deb3834ed15295c2ab7ef30cc8ca1f40feac86ef54a2fa3d26d48b13.js'));
  }
  if (key === '8761cef80882528c75d9104607f2031759acb0b348be3c8e5925f2bc791c7a66') {
    pending.push(import('./chunks/chunk-5d83060d6ec0714f597d390dc49e63ae364efeca02dd32eb53b1976effdc63c4.js'));
  }
  return Promise.all(pending);
}
window.Vaadin = window.Vaadin || {};
window.Vaadin.Flow = window.Vaadin.Flow || {};
window.Vaadin.Flow.loadOnDemand = loadOnDemand;
window.Vaadin.Flow.resetFocus = () => {
 let ae=document.activeElement;
 while(ae&&ae.shadowRoot) ae = ae.shadowRoot.activeElement;
 return !ae || ae.blur() || ae.focus() || true;
}