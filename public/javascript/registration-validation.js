/*
 * Copyright 2020 CJWW Development
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

$(document).ready(() => {
  let userNameState = false;
  let emailState = false;
  let passwordState = false;

  $('#userNameInput').keyup(() => {
    const id = 'userNameInput';
    let fieldVal = document.getElementById(id).value;
    let state = fieldVal.length > 1;
    evaluateValidityClasses(id, state);
    userNameState = state;
    evaluateValidity(userNameState, emailState, passwordState);
  });

  $('#emailInput').keyup(() => {
    const id = 'emailInput';
    let fieldVal = document.getElementById(id).value;
    let state = fieldVal.length > 1;
    evaluateValidityClasses(id, state);
    emailState = state;
    evaluateValidity(userNameState, emailState, passwordState);
  });

  $('#passwordInput').keyup(() => {
    const id = 'passwordInput';
    let fieldVal = document.getElementById(id).value;
    let state = fieldVal.length > 1;
    evaluateValidityClasses(id, state);
    evaluateValidity(userNameState, emailState, passwordState);
  });

  $('#confirmPasswordInput').keyup(() => {
    let passwordVal = document.getElementById('passwordInput').value;
    let confirmPasswordVal = document.getElementById('confirmPasswordInput').value;
    let state = passwordVal === confirmPasswordVal;
    evaluateValidityClasses('confirmPasswordInput', state);
    passwordState = state;
    evaluateValidity(userNameState, emailState, passwordState);
  });
});

const evaluateValidity = (userNameState, emailState, passwordState) => {
  userNameState && emailState && passwordState ? enableBtn() : disableBtn();
};

const enableBtn = () => {
  document.getElementById('registration-submit').disabled = false;
};

const disableBtn = () => {
  document.getElementById('registration-submit').disabled = true;
};

const evaluateValidityClasses = (id, boolean) => {
  boolean ? addIsValidClass(id) : addIsInValidClass(id);
};

const addIsValidClass = (id) => {
  document.getElementById(id).classList.add('is-valid');
  document.getElementById(id).classList.remove('is-invalid');
};

const addIsInValidClass = (id) => {
  document.getElementById(id).classList.add('is-invalid');
  document.getElementById(id).classList.remove('is-valid');
};