package com.ibaevzz.pcr.data.exceptions

class CouldNotDetermineAddress(bytes: Int, error: Int): PCRException("Не удалось прочесть адрес. Прочитано байт: $bytes, код ошибки: $error")