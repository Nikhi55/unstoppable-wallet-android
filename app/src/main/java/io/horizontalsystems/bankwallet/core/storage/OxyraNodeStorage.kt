package io.horizontalsystems.bankwallet.core.storage

import io.horizontalsystems.bankwallet.entities.OxyraNodeRecord

class OxyraNodeStorage(appDatabase: AppDatabase) {

    private val dao by lazy { appDatabase.oxyraNodeDao() }

    fun getAll() = dao.getAll()

    fun save(record: OxyraNodeRecord) {
        dao.insert(record)
    }

    fun delete(url: String) {
        dao.delete(url)
    }

}
