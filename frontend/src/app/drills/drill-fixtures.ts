import { DrillDetail, Stage, Step } from './drill.model';

/** Test data for the drill screens. Not used by the app. */

export function testStep(overrides: Partial<Step> & Pick<Step, 'id' | 'partId' | 'type'>): Step {
  return {
    path: [],
    targetPartId: '',
    after: '',
    afterEdge: 'END',
    delay: 0,
    speed: 0,
    duration: 0,
    sketch: 1,
    label: '',
    ...overrides,
  };
}

/** A1 (with the ball) passes to A2, who shoots at the top goal. */
export function testStage(name = 'Pass und Schuss', id = 's1'): Stage {
  return {
    id,
    name,
    sketches: [1],
    area: 'FULL',
    actors: [
      { id: 'a1', kind: 'PLAYER', side: 'A', label: 'A1', start: { x: -5, y: 0 }, hasBall: true },
      { id: 'a2', kind: 'PLAYER', side: 'A', label: 'A2', start: { x: 5, y: 0 }, hasBall: false },
    ],
    parts: [
      { id: 'p1', name: 'Passgeberin', actorId: 'a1', nextPartId: '' },
      { id: 'p2', name: 'Schützin', actorId: 'a2', nextPartId: '' },
    ],
    props: [{ id: 'c1', kind: 'CONE', position: { x: 0, y: -4 } }],
    steps: [
      testStep({ id: 'pass', partId: 'p1', type: 'PASS', path: [{ x: 5, y: 0 }], targetPartId: 'p2', speed: 10, label: '1' }),
      testStep({ id: 'shot', partId: 'p2', type: 'SHOT', path: [{ x: 0, y: 9.65 }], after: 'pass', delay: 0.5, speed: 25, label: '2' }),
    ],
    repetition: { mode: 'REPLAY', mirrored: false },
  };
}

export function testDrill(overrides: Partial<DrillDetail> = {}): DrillDetail {
  return {
    id: 'd-1',
    name: 'Bresil',
    status: 'READY',
    error: null,
    sketchRelation: 'PROGRESSION',
    tags: [{ id: 't-pass', name: 'Passen' }],
    sketches: [
      {
        position: 1,
        note: 'Start',
        reading: {
          sketch: 1,
          boardSide: 'FULL',
          orientation: 'aufrecht',
          symbols: [{ id: 's1-1', kind: 'CROSS', color: 'blau', points: [{ x: -5, y: 0 }], label: '', confidence: 0.9, meaning: 'Spielerin' }],
        },
      },
    ],
    currentVersion: 1,
    script: { stages: [testStage()], assumptions: ['Kein Goalie gezeichnet.'] },
    versions: [{ version: 1, source: 'AI', changeSummary: null, createdAt: '2026-09-27T10:00:00Z' }],
    messages: [
      { position: 1, author: 'INTERPRETER', content: 'Skript erstellt.', questions: null, answers: null, scriptVersion: 1, createdAt: '2026-09-27T10:00:00Z' },
    ],
    openQuestions: [],
    ...overrides,
  };
}
