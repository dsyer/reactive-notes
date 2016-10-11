/*
 * Copyright 2015-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example;

import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.ipc.netty.http.HttpClient;

@RestController
public class NettyClientController {

    @Value("${app.url:http://example.com}")
    private String url = "http://example.com";

    private static Logger log = LoggerFactory.getLogger(NettyClientController.class);
    private HttpClient client = HttpClient.create();

    @RequestMapping("/netty")
    public CompletableFuture<Result> gather() {
        log.info("Handling /netty");
        return Flux.range(1, 10) //
                .log() //
                .flatMap(value -> fetch(value)) //
                .collect(Result::new, Result::add) //
                .doOnSuccess(Result::stop) //
                .toFuture();
    }

    private Mono<HttpStatus> fetch(int value) {
        return this.client.get(url)
                .map(inbound -> HttpStatus.valueOf(inbound.status().code()));
    }
}
