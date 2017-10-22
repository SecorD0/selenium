// Licensed to the Software Freedom Conservancy (SFC) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The SFC licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

'use strict';

const assert = require('assert');

const build = require('./build');
const fileserver = require('./fileserver');
const firefox = require('../../firefox');
const isDevMode = require('../devmode');
const logging = require('../../lib/logging');
const remote = require('../../remote');
const safari = require('../../safari');
const testing = require('../../testing');
const webdriver = require('../../');

const NO_BUILD = /^1|true$/i.test(process.env['SELENIUM_NO_BUILD']);


/**
 * @param {function(!testing.Environment)} fn The top level suite function.
 * @param {testing.SuiteOptions=} options Suite specific options.
 */
function suite(fn, options = undefined) {
  testing.suite(function(env) {
    before(function() {
      if (isDevMode && !NO_BUILD) {
        return build.of(
            '//javascript/atoms/fragments:is-displayed',
            '//javascript/webdriver/atoms:get-attribute')
            .onlyOnce().go();
      }
    });

    fn(env);
  }, options);
}


// GLOBAL TEST SETUP


if (/^1|true$/i.test(process.env['SELENIUM_VERBOSE'])) {
  logging.installConsoleHandler();
  logging.getLogger('webdriver.http').setLevel(logging.Level.ALL);
}

testing.init();

before(function() {
   // Do not pass register fileserver.start directly with testing.before,
   // as start takes an optional port, which before assumes is an async
   // callback.
   return fileserver.start();
});

after(function() {
   return fileserver.stop();
});


// PUBLIC API


exports.suite = suite;
exports.ignore = testing.ignore;
exports.Pages = fileserver.Pages;
exports.whereIs = fileserver.whereIs;
