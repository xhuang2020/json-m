package net.json.jsonm

class MismatchException(message: String): RuntimeException(message)

class JsonValidationException(message: String): RuntimeException(message)

class JsonMatchValidationException(message: String): RuntimeException(message)