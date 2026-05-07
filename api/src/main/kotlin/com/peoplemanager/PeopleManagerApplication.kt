package com.peoplemanager

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class PeopleManagerApplication

fun main(args: Array<String>) {
    runApplication<PeopleManagerApplication>(*args)
}
