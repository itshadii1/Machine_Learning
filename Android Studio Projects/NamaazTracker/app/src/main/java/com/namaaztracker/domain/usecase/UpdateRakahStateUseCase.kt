package com.namaaztracker.domain.usecase

import com.namaaztracker.domain.model.NamaazSession
import com.namaaztracker.domain.model.NamaazType
import com.namaaztracker.domain.model.Posture
import com.namaaztracker.domain.model.RakahState
import javax.inject.Inject

class UpdateRakahStateUseCase @Inject constructor() {

    operator fun invoke(posture: Posture, session: NamaazSession): NamaazSession =
        when (session.currentState) {
            RakahState.IDLE -> session

            RakahState.QIAM ->
                if (posture == Posture.RUKOOH) session.copy(currentState = RakahState.RUKOOH)
                else session

            RakahState.RUKOOH ->
                if (posture == Posture.QIAM) session.copy(currentState = RakahState.ITIDAL)
                else session

            RakahState.ITIDAL ->
                if (posture == Posture.SAJDAH) session.copy(currentState = RakahState.SAJDAH_1)
                else session

            RakahState.SAJDAH_1 ->
                if (posture == Posture.JULSA) session.copy(currentState = RakahState.JULSA)
                else session

            RakahState.JULSA ->
                if (posture == Posture.SAJDAH) advanceRakah(session)
                else session

            RakahState.TASHAHHUD ->
                if (posture == Posture.QIAM) session.copy(currentState = RakahState.QIAM)
                else session

            RakahState.COMPLETE -> session
        }

    private fun advanceRakah(session: NamaazSession): NamaazSession {
        val newCount = session.completedRakat + 1
        val total = session.namaazType.rakahCount

        val nextState = when {
            newCount >= total -> RakahState.COMPLETE
            isTashahhudPoint(newCount, session.namaazType) -> RakahState.TASHAHHUD
            else -> RakahState.QIAM
        }

        return session.copy(completedRakat = newCount, currentState = nextState)
    }

    private fun isTashahhudPoint(completedRakat: Int, namaazType: NamaazType): Boolean =
        when (namaazType) {
            NamaazType.FAJR -> false
            NamaazType.MAGHRIB -> completedRakat == 2
            NamaazType.ZUHR, NamaazType.ASR, NamaazType.ISHA -> completedRakat == 2
        }
}
