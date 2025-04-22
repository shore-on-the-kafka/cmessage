package me.chacham.cmessage.common.util

import org.slf4j.LoggerFactory

fun Any.logger() = LoggerFactory.getLogger(this@logger::class.java)
