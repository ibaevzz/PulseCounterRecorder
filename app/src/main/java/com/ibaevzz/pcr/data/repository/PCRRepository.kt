package com.ibaevzz.pcr.data.repository

import com.ibaevzz.pcr.domain.repository.CloseConnectionDevice
import com.ibaevzz.pcr.domain.repository.ConnectToDevice

interface PCRRepository: ConnectToDevice, CloseConnectionDevice