/*
 * Copyright 2018-2019 ABSA Group Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

sap.ui.define([
  "sap/ui/core/mvc/Controller",
  "sap/m/MessageToast"
], function (Controller, MessageToast) {
  "use strict";

  const usernameField = "username";
  const passwordField = "password";

  return Controller.extend("components.login.loginMain", {
    loginForm: {},

    /**
     * Called when a controller is instantiated and its View controls (if
     * available) are already created. Can be used to modify the View before it
     * is displayed, to bind event handlers and do other one-time
     * initialization.
     *
     * @memberOf components.login.loginMain
     */
    onInit: function () {
      this._router = sap.ui.core.UIComponent.getRouterFor(this);
      let model = {username: "", password: "", submit: "Login"};

      this.loginForm = this.byId("loginForm");
      this.loginForm.setModel(new sap.ui.model.json.JSONModel(model), "login");

      this.byId("loginSubmit").attachPress(this.onLoginSubmit, this);
    },

    onLoginSubmit: function (oEvent) {
      let oData = this.loginForm.getModel("login").oData;

      this._resetLoginFormState();
      if (this._validateLogin(oData)) {
        this._login(oData, this.byId("loginSubmit"))
      }
    },

    _resetFieldState: function (sField) {
      this.byId(sField).setValueState(sap.ui.core.ValueState.None);
      this.byId(sField).setValueStateText("");
    },

    _resetLoginFormState: function () {
      this._resetFieldState(usernameField);
      this._resetFieldState(passwordField);
    },

    _validateField: function (oData, sField, sErrorMessage) {
      let isOk = oData[sField] && oData[sField] !== "";

      if (!isOk) {
        let field = this.byId(sField);
        field.setValueState(sap.ui.core.ValueState.Error);
        field.setValueStateText(sErrorMessage);
      }

      return isOk;
    },

    _validateLogin(oData) {
      let isValidUsername = this._validateField(oData, usernameField, "Username cannot be empty.");
      let isValidPassword = this._validateField(oData, passwordField, "Password cannot be empty.");
      return isValidUsername && isValidPassword;
    },

    _login: function (oData, oControl) {
      if (oControl) oControl.setBusy(true);

      let fnSuccess = (result, status, xhr) => {
        let csrfToken = xhr.getResponseHeader("X-CSRF-TOKEN");
        localStorage.setItem("csrfToken", csrfToken);
        Functions.ajax("api/user/info", "GET", {}, (oInfo) => {
          model.setProperty("/userInfo", oInfo);
          this._router.navTo("schemas");
        });
      };

      let fnError = () => {
        MessageToast.show("Username or password incorrect");
        this.byId(usernameField).setValueState(sap.ui.core.ValueState.Error);
        this.byId(passwordField).setValueState(sap.ui.core.ValueState.Error);
      };
      $.ajax("api/login", {
        complete: function () {
          if (oControl) oControl.setBusy(false)
        },
        data: oData,
        method: "POST",
        success: fnSuccess,
        error: fnError
      })
    },
    /**
     * Similar to onAfterRendering, but this hook is invoked before the controller's
     * View is re-rendered (NOT before the first rendering! onInit() is used for
     * that one!).
     *
     * @memberOf components.schema.schemaMain
     */
    // onBeforeRendering: function () {
    //
    // },
    /**
     * Called when the View has been rendered (so its HTML is part of the document).
     * Post-rendering manipulations of the HTML could be done here. This hook is the
     * same one that SAPUI5 controls get after being rendered.
     *
     * @memberOf components.schema.schemaMain
     */
    // onAfterRendering: function() {
    //
    // },
    /**
     * Called when the Controller is destroyed. Use this one to free resources and
     * finalize activities.
     *
     * @memberOf components.schema.schemaMain
     */
    // onExit: function() {
    //
    // }
  });
});
