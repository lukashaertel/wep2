package eu.metatools.fio

import eu.metatools.fio.immediate.Capture
import eu.metatools.fio.immediate.Draw
import eu.metatools.fio.immediate.Play
import eu.metatools.fio.queued.QueuedCapture
import eu.metatools.fio.queued.QueuedDraw
import eu.metatools.fio.queued.QueuedPlay

interface InOut : Capture,
    Draw, Play,
    QueuedCapture, QueuedDraw,
    QueuedPlay