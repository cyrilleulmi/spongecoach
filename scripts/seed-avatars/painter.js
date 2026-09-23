/*
 * Browser-side drawing for the seeded Avatars (see draw-avatars.mjs): hand-drawn smileys. A round
 * face with dot eyes and a curved mouth, drawn with slightly wobbly ink lines, plus as much or as
 * little doodling as a Player's spec asks for — some are a bare smiley, some get hair, glasses and
 * a floorball stick.
 *
 * window.drawAvatar(canvas, spec) paints one 512 px portrait; a spec is a plain object:
 *   { seed, bg, skin, hair?: { style, color }, eyes, mouth, extras: [] }
 */
(() => {
  const S = 512;
  const INK = '#2a1f1d';
  const FACE = { x: 256, y: 266, r: 150 };

  function makeRandom(seed) {
    let state = seed | 0;
    return () => {
      state = (state + 0x6d2b79f5) | 0;
      let t = Math.imul(state ^ (state >>> 15), 1 | state);
      t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t;
      return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
    };
  }

  function shade(hex, amount) {
    const n = parseInt(hex.slice(1), 16);
    const channel = (shift) => Math.max(0, Math.min(255, ((n >> shift) & 255) + amount));
    return `rgb(${channel(16)}, ${channel(8)}, ${channel(0)})`;
  }

  function drawAvatar(canvas, spec) {
    const ctx = canvas.getContext('2d');
    const random = makeRandom(spec.seed);
    const jitter = (amount) => (random() - 0.5) * 2 * amount;
    const has = (extra) => spec.extras.includes(extra);
    const { x: fx, y: fy, r } = FACE;

    // --- hand-drawn primitives ----------------------------------------------------

    /** A wobbly ellipse, as if drawn freehand. */
    function blob(cx, cy, rx, ry, fill, { lw = 7, wobble = 0.03, rotate = 0 } = {}) {
      const phaseA = random() * Math.PI * 2;
      const phaseB = random() * Math.PI * 2;
      ctx.beginPath();
      for (let i = 0; i <= 60; i++) {
        const a = (i / 60) * Math.PI * 2;
        const k = 1 + wobble * (Math.sin(a * 2 + phaseA) * 0.6 + Math.sin(a * 5 + phaseB) * 0.4);
        const x = Math.cos(a) * rx * k;
        const y = Math.sin(a) * ry * k;
        const px = cx + x * Math.cos(rotate) - y * Math.sin(rotate);
        const py = cy + x * Math.sin(rotate) + y * Math.cos(rotate);
        i === 0 ? ctx.moveTo(px, py) : ctx.lineTo(px, py);
      }
      ctx.closePath();
      if (fill) {
        ctx.fillStyle = fill;
        ctx.fill();
      }
      if (lw > 0) {
        ctx.lineWidth = lw;
        ctx.strokeStyle = INK;
        ctx.lineJoin = 'round';
        ctx.stroke();
      }
    }

    /** A pen line through points, smoothed, with a touch of hand wobble. */
    function line(points, width = 8, color = INK, alpha = 1) {
      const pts = points.map(([x, y]) => [x + jitter(1.5), y + jitter(1.5)]);
      ctx.save();
      ctx.globalAlpha = alpha;
      ctx.strokeStyle = color;
      ctx.lineWidth = width;
      ctx.lineCap = 'round';
      ctx.lineJoin = 'round';
      ctx.beginPath();
      ctx.moveTo(pts[0][0], pts[0][1]);
      for (let i = 1; i < pts.length - 1; i++) {
        ctx.quadraticCurveTo(pts[i][0], pts[i][1], (pts[i][0] + pts[i + 1][0]) / 2, (pts[i][1] + pts[i + 1][1]) / 2);
      }
      ctx.lineTo(pts[pts.length - 1][0], pts[pts.length - 1][1]);
      ctx.stroke();
      ctx.restore();
    }

    /** A closed, filled, inked shape through points. */
    function shape(points, fill, lw = 7) {
      ctx.beginPath();
      ctx.moveTo((points[0][0] + points[1][0]) / 2, (points[0][1] + points[1][1]) / 2);
      for (let i = 1; i <= points.length; i++) {
        const p = points[i % points.length];
        const n = points[(i + 1) % points.length];
        ctx.quadraticCurveTo(p[0], p[1], (p[0] + n[0]) / 2, (p[1] + n[1]) / 2);
      }
      ctx.closePath();
      ctx.fillStyle = fill;
      ctx.fill();
      if (lw > 0) {
        ctx.lineWidth = lw;
        ctx.strokeStyle = INK;
        ctx.lineJoin = 'round';
        ctx.stroke();
      }
    }

    function star(cx, cy, size, color) {
      const pts = [];
      for (let i = 0; i < 10; i++) {
        const radius = i % 2 === 0 ? size : size * 0.45;
        const a = (i / 10) * Math.PI * 2 - Math.PI / 2;
        pts.push([cx + Math.cos(a) * radius, cy + Math.sin(a) * radius]);
      }
      ctx.beginPath();
      pts.forEach(([x, y], i) => (i === 0 ? ctx.moveTo(x, y) : ctx.lineTo(x, y)));
      ctx.closePath();
      ctx.fillStyle = color;
      ctx.fill();
      ctx.lineWidth = 5;
      ctx.lineJoin = 'round';
      ctx.strokeStyle = INK;
      ctx.stroke();
    }

    function heart(cx, cy, size, color) {
      shape(
        [[cx, cy + size], [cx - size * 1.3, cy - size * 0.1], [cx - size * 0.7, cy - size], [cx, cy - size * 0.35],
         [cx + size * 0.7, cy - size], [cx + size * 1.3, cy - size * 0.1]],
        color, 5,
      );
    }

    // --- scene --------------------------------------------------------------------

    function background() {
      ctx.fillStyle = spec.bg;
      ctx.fillRect(0, 0, S, S);
    }

    /** Hair that sits behind the face: long hair, ponytails, buns. */
    function hairBack() {
      const hair = spec.hair;
      if (!hair) return;
      const c = hair.color;
      if (hair.style === 'long') {
        shape([[fx - r - 10, fy - 60], [fx - r - 26, fy + 90], [fx - r + 10, fy + 190], [fx - 40, fy + 120],
               [fx + 40, fy + 120], [fx + r - 10, fy + 190], [fx + r + 26, fy + 90], [fx + r + 10, fy - 60], [fx, fy - r - 30]], c);
      } else if (hair.style === 'bob') {
        shape([[fx - r - 18, fy - 40], [fx - r - 20, fy + 70], [fx - r + 30, fy + 95], [fx + r - 30, fy + 95],
               [fx + r + 20, fy + 70], [fx + r + 18, fy - 40], [fx, fy - r - 30]], c);
      } else if (hair.style === 'bun') {
        blob(fx + 10, fy - r - 18, 56, 48, c);
      } else if (hair.style === 'ponytail') {
        const s = hair.side ?? 1;
        shape([[fx + s * 110, fy - 90], [fx + s * 205, fy - 60], [fx + s * 225, fy + 40], [fx + s * 190, fy + 120], [fx + s * 150, fy + 10]], c);
        blob(fx + s * 128, fy - 92, 17, 14, hair.tie ?? '#e76f51', { lw: 5 });
      } else if (hair.style === 'curly') {
        for (let i = 0; i < 16; i++) {
          const a = Math.PI * 0.85 + (i / 15) * Math.PI * 1.3;
          blob(fx + Math.cos(a) * (r + 6), fy + Math.sin(a) * (r + 6), 36, 34, c, { lw: 6 });
        }
      }
    }

    function face() {
      blob(fx, fy, r, r, spec.skin, { lw: 9, wobble: 0.02 });
    }

    /** Hair on top of the face: a fringe cap (all 'short' hair is) or a few doodled strands. */
    function hairFront() {
      const hair = spec.hair;
      if (!hair) return;
      const c = hair.color;
      if (hair.style === 'strands') {
        // Just a few doodled curls on top — the simplest hair there is.
        line([[fx - 40, fy - r + 6], [fx - 30, fy - r - 40], [fx - 6, fy - r - 22]], 9, c);
        line([[fx + 4, fy - r + 2], [fx + 14, fy - r - 48], [fx + 40, fy - r - 26]], 9, c);
        return;
      }
      const top = [];
      for (let i = 0; i <= 10; i++) {
        const a = Math.PI * 1.08 + (i / 10) * Math.PI * 0.84;
        top.push([fx + Math.cos(a) * (r + 8), fy + Math.sin(a) * (r + 8)]);
      }
      const fringeY = fy - r * 0.45;
      const fringe =
        hair.style === 'bob' || hair.style === 'long'
          ? [[fx + r * 0.75, fringeY + 10], [fx + 20, fringeY - 20], [fx - r * 0.3, fringeY + 6], [fx - r * 0.85, fringeY + 22]]
          : [[fx + r * 0.7, fringeY - 16], [fx + 30, fringeY - 36], [fx - 30, fringeY - 30], [fx - r * 0.7, fringeY - 12]];
      shape([...top, ...fringe], c);
      line([[fx - 50, fy - r + 10], [fx - 10, fy - r - 2], [fx + 30, fy - r + 8]], 7, shade(c, 60), 0.7);
    }

    function eyes() {
      const ey = fy - 18;
      const dx = 52;
      const kind = spec.eyes;
      const dot = (x) => {
        blob(x, ey, 14, 19, INK, { lw: 0 });
        blob(x + 5, ey - 7, 4.5, 5, '#ffffff', { lw: 0 });
      };
      const arc = (x, up) =>
        line(up ? [[x - 18, ey + 8], [x, ey - 12], [x + 18, ey + 8]] : [[x - 18, ey - 4], [x, ey + 12], [x + 18, ey - 4]], 8);
      if (kind === 'happy') {
        arc(fx - dx, true);
        arc(fx + dx, true);
      } else if (kind === 'closed') {
        arc(fx - dx, false);
        arc(fx + dx, false);
      } else if (kind === 'wink') {
        dot(fx - dx);
        arc(fx + dx, true);
      } else if (kind === 'sparkle') {
        for (const x of [fx - dx, fx + dx]) {
          blob(x, ey, 18, 23, INK, { lw: 0 });
          star(x + 5, ey - 7, 8, '#ffffff');
        }
      } else {
        dot(fx - dx);
        dot(fx + dx);
      }
      if (has('lashes')) {
        for (const x of [fx - dx, fx + dx]) {
          const side = x < fx ? -1 : 1;
          line([[x + side * 14, ey - 16], [x + side * 24, ey - 26]], 5);
          line([[x + side * 4, ey - 20], [x + side * 8, ey - 32]], 5);
        }
      }
      if (has('fierce')) {
        line([[fx - dx - 24, ey - 44], [fx - dx + 20, ey - 30]], 9);
        line([[fx + dx + 24, ey - 44], [fx + dx - 20, ey - 30]], 9);
      } else if (has('brows')) {
        line([[fx - dx - 20, ey - 36], [fx - dx, ey - 46], [fx - dx + 20, ey - 38]], 8);
        line([[fx + dx - 20, ey - 38], [fx + dx, ey - 46], [fx + dx + 20, ey - 36]], 8);
      }
    }

    function cheeks() {
      if (!has('blush') && !has('freckles')) return;
      if (has('blush')) {
        ctx.save();
        ctx.globalAlpha = 0.4;
        blob(fx - 88, fy + 30, 24, 14, '#ff7b8a', { lw: 0 });
        blob(fx + 88, fy + 30, 24, 14, '#ff7b8a', { lw: 0 });
        ctx.restore();
      }
      if (has('freckles')) {
        for (const side of [-1, 1]) {
          for (const [ox, oy] of [[70, 20], [86, 32], [100, 18], [80, 44]]) {
            blob(fx + side * ox, fy + oy, 3, 3, shade(spec.skin, -80), { lw: 0 });
          }
        }
      }
    }

    function mouth() {
      const my = fy + 52;
      const lips = spec.lips ?? '#8c2f39';
      switch (spec.mouth) {
        case 'smile':
          line([[fx - 58, my - 8], [fx, my + 32], [fx + 58, my - 8]], 9);
          break;
        case 'smirk':
          line([[fx - 48, my + 10], [fx + 10, my + 20], [fx + 56, my - 10]], 9);
          break;
        case 'o':
          blob(fx, my + 12, 22, 26, lips, { lw: 7 });
          break;
        case 'tongue':
          line([[fx - 58, my - 8], [fx, my + 32], [fx + 58, my - 8]], 9);
          shape([[fx + 4, my + 26], [fx + 44, my + 14], [fx + 48, my + 44], [fx + 24, my + 58]], '#ff7b8a', 6);
          break;
        default: {
          // 'grin' and 'laugh': an open D-shaped mouth.
          const w = spec.mouth === 'laugh' ? 66 : 56;
          const d = spec.mouth === 'laugh' ? 60 : 44;
          shape([[fx - w, my - 12], [fx - w * 0.6, my + d], [fx + w * 0.6, my + d], [fx + w, my - 12]], lips);
          line([[fx - w + 14, my - 4], [fx + w - 14, my - 4]], 10, '#ffffff');
        }
      }
    }

    function accessories() {
      const ey = fy - 18;
      if (has('glasses')) {
        ctx.save();
        ctx.lineWidth = 8;
        ctx.strokeStyle = spec.glassesColor ?? INK;
        for (const x of [fx - 52, fx + 52]) {
          ctx.beginPath();
          ctx.arc(x, ey, 36, 0, Math.PI * 2);
          ctx.stroke();
        }
        ctx.restore();
        line([[fx - 16, ey - 4], [fx, ey - 10], [fx + 16, ey - 4]], 7);
      }
      if (has('sunglasses')) {
        for (const x of [fx - 54, fx + 54]) {
          shape([[x - 42, ey - 22], [x + 42, ey - 22], [x + 36, ey + 20], [x - 36, ey + 20]], '#1b2430', 6);
          line([[x - 26, ey - 10], [x - 8, ey - 10]], 6, '#9ad1f0');
        }
        line([[fx - 14, ey - 16], [fx + 14, ey - 16]], 8);
      }
      if (has('earrings')) {
        for (const side of [-1, 1]) {
          blob(fx + side * (r - 4), fy + 40, 10, 10, '#f2c94c', { lw: 5 });
        }
      }
      if (has('headband')) {
        shape([[fx - r + 6, fy - 78], [fx, fy - 112], [fx + r - 6, fy - 78], [fx + r - 2, fy - 52], [fx, fy - 84], [fx - r + 2, fy - 52]],
          spec.headbandColor ?? '#e76f51');
      }
      if (has('bandaid')) {
        ctx.save();
        ctx.translate(fx + 96, fy - 2);
        ctx.rotate(-0.5);
        shape([[-26, -11], [26, -11], [26, 11], [-26, 11]], '#f3d3b0', 5);
        line([[-6, 0], [6, 0]], 4, '#c9a27e');
        ctx.restore();
      }
      if (has('cage')) {
        // A goalie mask: a band over the forehead and a wire cage in front of the face.
        shape([[fx - r - 4, fy - 40], [fx - r + 10, fy - 120], [fx, fy - r - 16], [fx + r - 10, fy - 120], [fx + r + 4, fy - 40],
               [fx + r - 20, fy - 70], [fx, fy - 96], [fx - r + 20, fy - 70]], spec.helmetColor ?? '#f4a261');
        ctx.save();
        ctx.strokeStyle = '#7d848c';
        ctx.lineWidth = 7;
        ctx.lineCap = 'round';
        for (const bx of [-92, 0, 92]) {
          ctx.beginPath();
          ctx.moveTo(fx + bx, fy - 72);
          ctx.quadraticCurveTo(fx + bx * 1.1, fy + 40, fx + bx * 0.7, fy + 128);
          ctx.stroke();
        }
        for (const by of [26, 100]) {
          ctx.beginPath();
          ctx.moveTo(fx - r + 16, fy + by);
          ctx.quadraticCurveTo(fx, fy + by + 18, fx + r - 16, fy + by);
          ctx.stroke();
        }
        ctx.restore();
      }
    }

    /** Doodles around the face, in the free corners of the square. */
    function doodles() {
      if (has('stick')) {
        line([[404, 470], [456, 120]], 16);
        line([[404, 470], [456, 120]], 8, '#ffffff');
        shape([[446, 124], [490, 94], [502, 118], [462, 144]], '#e63946', 6);
        blob(78, 132, 26, 26, '#ffffff', { lw: 6 });
        for (const [hx, hy] of [[70, 124], [86, 132], [74, 144], [88, 116]]) {
          blob(hx, hy, 3.4, 3.4, '#9aa1a8', { lw: 0 });
        }
        line([[26, 168], [52, 156]], 6, INK, 0.6);
        line([[20, 190], [48, 176]], 6, INK, 0.6);
      }
      if (has('stars')) {
        star(80, 90, 26, '#ffd23f');
        star(440, 440, 18, '#ffffff');
      }
      if (has('star')) {
        star(430, 86, 24, '#ffd23f');
      }
      if (has('hearts')) {
        heart(84, 96, 22, '#ff5d73');
        heart(438, 420, 16, '#ff5d73');
      }
      if (has('shout')) {
        ctx.save();
        ctx.font = '900 96px "Arial Black", Arial, sans-serif';
        ctx.lineWidth = 9;
        ctx.strokeStyle = INK;
        ctx.strokeText('!', 410, 150);
        ctx.fillStyle = '#ffd23f';
        ctx.fillText('!', 410, 150);
        ctx.restore();
        line([[440, 250], [484, 236]], 8);
        line([[440, 290], [488, 296]], 8);
      }
      if (has('bolt')) {
        shape([[424, 54], [382, 150], [420, 150], [396, 238], [470, 126], [430, 126], [458, 54]], '#ffd23f', 6);
      }
      if (has('sweat')) {
        shape([[412, 150], [432, 190], [424, 210], [402, 206], [398, 186]], '#8fd3f4', 6);
      }
      if (has('bubble')) {
        shape([[340, 40], [480, 40], [490, 104], [420, 110], [388, 136], [396, 108], [334, 104]], '#ffffff', 7);
        for (const dx of [0, 34, 68]) {
          blob(378 + dx, 74, 7, 7, INK, { lw: 0 });
        }
      }
      if (has('music')) {
        line([[430, 70], [430, 140]], 8);
        line([[470, 58], [470, 128]], 8);
        line([[430, 70], [470, 58]], 12);
        blob(418, 142, 14, 11, INK, { lw: 0, rotate: -0.4 });
        blob(458, 130, 14, 11, INK, { lw: 0, rotate: -0.4 });
      }
      if (has('sparkles')) {
        for (const [sx, sy, s] of [[70, 110, 16], [446, 90, 12], [440, 430, 14]]) {
          line([[sx - s, sy], [sx + s, sy]], 6, '#ffffff');
          line([[sx, sy - s], [sx, sy + s]], 6, '#ffffff');
        }
      }
    }

    background();
    hairBack();
    face();
    cheeks();
    eyes();
    mouth();
    hairFront();
    accessories();
    doodles();
  }

  window.drawAvatar = drawAvatar;
})();
