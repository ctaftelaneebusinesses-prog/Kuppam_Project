package com.onetowncity.app.core.data.auth

class FakeSessionStore(var session: Session? = null, var verifier: String? = null) : SessionStore {
    override fun load() = session
    override fun save(session: Session) { this.session = session }
    override fun clear() { session = null }
    override fun loadPendingVerifier() = verifier
    override fun savePendingVerifier(verifier: String) { this.verifier = verifier }
    override fun clearPendingVerifier() { verifier = null }
}
