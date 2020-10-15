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

package cube.console.container;

import cell.util.log.LogLevel;
import cell.util.log.LogManager;
import cell.util.log.Logger;
import cube.console.Console;
import cube.console.ReportHandler;
import cube.console.container.handler.ServerLogHandler;
import cube.console.container.handler.ServerReportHandler;
import cube.console.container.handler.ServersHandler;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;

import java.io.File;

public class Main {

    public Main() {
    }

    /**
     *
     * @param port
     */
    public static void start(int port) {
        LogManager.getInstance().setLevel(LogLevel.DEBUG);
        LogManager.getInstance().addHandle(LogManager.createSystemOutHandle());

        Console console = new Console();
        console.launch();

        Server server = new Server(port);

        ResourceHandler resourceHandler = new ResourceHandler();
        resourceHandler.setDirectoriesListed(false);
        resourceHandler.setWelcomeFiles(new String[] { "index.html" });

        // 判断目录
        File path = new File("web");
        if (path.exists() && path.isDirectory()) {
            resourceHandler.setResourceBase("web");
        }
        else {
            resourceHandler.setResourceBase("WebContent");
        }

        ContextHandler indexHandler = new ContextHandler("/");
        indexHandler.setHandler(resourceHandler);

        HandlerList handlers = new HandlerList();
        handlers.setHandlers(new Handler[] {
                indexHandler,

                // For RESTful API
                new ReportHandler(console),

                // For AJAX API
                new ServersHandler(console),
                new ServerLogHandler(console),
                new ServerReportHandler(console),

                new StopHandler(server, console),
                new DefaultHandler()});

        server.setHandler(handlers);

        Logger.i(Main.class, "Start cube console server # " + port);

        System.out.println("" +
                "                                             \n" +
                "  ,----..                                    \n" +
                " /   /   \\                ,---,              \n" +
                "|   :     :         ,--,,---.'|              \n" +
                ".   |  ;. /       ,'_ /||   | :              \n" +
                ".   ; /--`   .--. |  | ::   : :      ,---.   \n" +
                ";   | ;    ,'_ /| :  . |:     |,-.  /     \\  \n" +
                "|   : |    |  ' | |  . .|   : '  | /    /  | \n" +
                ".   | '___ |  | ' |  | ||   |  / :.    ' / | \n" +
                "'   ; : .'|:  | : ;  ; |'   : |: |'   ;   /| \n" +
                "'   | '/  :'  :  `--'   \\   | '/ :'   |  / | \n" +
                "|   :    / :  ,      .-./   :    ||   :    | \n" +
                " \\   \\ .'   `--`----'   /    \\  /  \\   \\  /  \n" +
                "  `---`                 `-'----'    `----'   \n" +
                "                                             ");

        System.out.println("Enter \"http://Your-Server-IP:" + port + "\" in your browser to login Cube Console.");
        System.out.println();
        System.out.println("在浏览器中输入 \"http://您的服务器IP:" + port + "\" 登录 Cube Console 。");
        System.out.println();

        try {
            server.start();
            server.join();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * @param port
     */
    public static void stop(int port) {
        HttpClient httpClient = new HttpClient();
        try {
            httpClient.start();
            ContentResponse response = httpClient.GET("http://127.0.0.1:" + port + "/stop/");
            if (HttpStatus.OK_200 == response.getStatus()) {
                System.out.println("Cube Console - stop server # " + port);
            }
            else {
                System.out.println("Cube Console - STOP FAILED # " + port);
            }
        } catch (Exception e) {
            System.err.println("Cube Console - STOP FAILED # " + port + " - " + e.getMessage());
            //e.printStackTrace();
        } finally {
            try {
                httpClient.stop();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        if (args.length == 1) {
            if (args[0].equalsIgnoreCase("start")) {
                Main.start(7080);
            }
            else if (args[0].equalsIgnoreCase("stop")) {
                Main.stop(7080);
            }
        }
        else {
            System.out.println("Please input args");
        }
    }
}
