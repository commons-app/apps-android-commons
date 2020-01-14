package fr.free.nrw.commons.nearby.model

class ResultTuple {
    val type: String
    val value: String

    constructor(type: String, value: String) {
        this.type = type
        this.value = value
    }

    constructor() {
        type = ""
        value = ""
    }

}