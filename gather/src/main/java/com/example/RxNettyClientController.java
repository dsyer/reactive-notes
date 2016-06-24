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

import io.netty.buffer.ByteBuf;
import io.reactivex.netty.protocol.http.client.HttpClient;
import reactor.core.converter.RxJava1ObservableConverter;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
public class RxNettyClientController {

    @Value("${app.host:example.com}")
    private String host = "example.com";

    private static Logger log = LoggerFactory.getLogger(RxNettyClientController.class);

    @RequestMapping("/rxnetty")
    public CompletableFuture<Result> gather() {
        log.info("Handling /rxnetty");
        return Flux.range(1, 10) //
                .log() //
                .flatMap(value -> fetch(value)) //
                .collect(Result::new, Result::add) //
                .doOnSuccess(Result::stop) //
                .toFuture();
    }

    private Mono<HttpStatus> fetch(int value) {
        HttpClient<ByteBuf, ByteBuf> client = HttpClient.newClient(host, 80);
        return RxJava1ObservableConverter
                .from(client.createGet("/")
                        .map(inbound -> HttpStatus.valueOf(inbound.getStatus().code())))
                .next();
    }
}
