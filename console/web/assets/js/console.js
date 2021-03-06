/**
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2021 Shixin Cube Team.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

/**
 * Console
 */
function Console() {
    this.user = null;

    this.dispatchers = null;
    this.services = null;
}

Console.prototype.checkUser = function(handler) {
    var token = null;
    var val = document.cookie;
    var array = val.split(';');
    for (var i = 0; i < array.length; ++i) {
        val = array[i].split('=');
        if (val.length == 2) {
            if (val[0] == 'CubeConsoleToken') {
                token = val[1].trim();
                break;
            }
        }
    }

    if (null == token) {
        handler(false);
        return;
    }

    var that = this;

    $.ajax({
        type: 'post',
        url: '/signin/'
    }).done(function(response) {
        that.user = response.user;
        handler(true);
    }).fail(function() {
        handler(false);
    });

    return false;
}

Console.prototype.log = function(text) {
    window.console.log(text);
}

Console.prototype.getDispatchers = function(handler) {
    $.get('/servers/dispatcher', function(response, status, xhr) {
        handler(response.tag, response.list);
    }, 'json');
}

Console.prototype.getDispatcherDefaultDeploy = function(handler) {
    $.get('/deploy/dispatcher', function(response, status, xhr) {
        handler(response);
    }, 'json');
}

Console.prototype.getServers = function(handler) {
    var that = this;
    $.get('/servers', function(response, status, xhr) {
        that.dispatchers = response.dispatchers;
        that.services = response.services;
        handler(response);
    }, 'json');
}

Console.prototype.queryConsoleLog = function(start, handler) {
    $.get('/log/console', { "start": start }, function(response, status, xhr) {
        handler(response);
    }, 'json');
}

Console.prototype.queryLog = function(name, start, handler) {
    $.get('/log/server', { "name": name, "start": start }, function(response, status, xhr) {
        handler(response);
    }, 'json');
}

Console.prototype.queryJVMReport = function(name, num, handler) {
    $.get('/server-report', { "report": "JVMReport", "name": name, "num": num }, function(response, status, xhr) {
        handler(response);
    }, 'json');
}
