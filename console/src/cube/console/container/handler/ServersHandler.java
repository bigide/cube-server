/**
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 Shixin Cube Team.
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

package cube.console.container.handler;

import cell.util.json.JSONArray;
import cell.util.json.JSONException;
import cell.util.json.JSONObject;
import cube.console.Console;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.handler.ContextHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 服务器信息句柄。
 */
public class ServersHandler extends ContextHandler  {

    private Console console;

    public ServersHandler(Console console) {
        super("/servers");
        this.setHandler(new Handler());
        this.console = console;
    }

    protected class Handler extends AbstractHandler {

        public Handler() {
            super();
        }

        @Override
        public void handle(String target, Request request,
                           HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse)
                throws IOException, ServletException {
            if (target.equals("/dispatcher")) {
                JSONArray list = console.getDispatcherServers();
                httpServletResponse.setStatus(HttpStatus.OK_200);
                httpServletResponse.getWriter().write(list.toString());
                request.setHandled(true);
            }
            else if (target.equals("/service")) {
                JSONArray list = console.getServiceServers();
                httpServletResponse.setStatus(HttpStatus.OK_200);
                httpServletResponse.getWriter().write(list.toString());
                request.setHandled(true);
            }
            else {
                JSONArray dispatcherList = console.getDispatcherServers();
                JSONArray serviceList = console.getServiceServers();
                JSONObject data = new JSONObject();
                try {
                    data.put("dispatchers", dispatcherList);
                    data.put("services", serviceList);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                httpServletResponse.setStatus(HttpStatus.OK_200);
                httpServletResponse.getWriter().write(data.toString());
                request.setHandled(true);
            }
        }
    }
}