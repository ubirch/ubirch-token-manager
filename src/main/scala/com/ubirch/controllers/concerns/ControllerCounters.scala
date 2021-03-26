package com.ubirch.controllers.concerns

import io.prometheus.client.Counter

object ControllerCounters {

  val successCounter: Counter = Counter.build()
    .name("token_management_success")
    .help("Represents the number of token management successes")
    .labelNames("service", "method", "version")
    .register()

  val errorCounter: Counter = Counter.build()
    .name("token_management_failures")
    .help("Represents the number of token management failures")
    .labelNames("service", "method", "version")
    .register()

}
