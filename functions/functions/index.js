import functions from 'firebase-functions';
import admin from 'firebase-admin';
import fetch from 'node-fetch';
try { admin.initializeApp(); } catch(e) {}
const db = admin.firestore();

function utcDay(ts){ const d=new Date(ts); const p=n=>n<10?'0'+n:n; return d.getUTCFullYear()+'-'+p(d.getUTCMonth()+1)+'-'+p(d.getUTCDate()); }
function startOfPrevDayUTC(){ const d=new Date(); d.setUTCHours(0,0,0,0); d.setUTCDate(d.getUTCDate()-1); return d.getTime(); }
function endOfPrevDayUTC(){ const d=new Date(); d.setUTCHours(0,0,0,0); return d.getTime()-1; }

export const rollupDailySummary = functions.pubsub.schedule('0 0 * * *').timeZone('Asia/Jakarta').onRun(async () => {
  const devices = await db.collection('devices').get();
  for (const dev of devices.docs) {
    const devId = dev.id;
    const from = startOfPrevDayUTC(), to = endOfPrevDayUTC();
    const evs = await db.collection('devices').doc(devId).collection('events')
      .where('ts','>=',from).where('ts','<=',to).get();
    const events = evs.docs.map(d=>d.data());

    const notif = events.filter(e=>e.type==='notification').length;
    const apps = {}; events.filter(e=>e.type==='app_foreground').forEach(e=>apps[e.app]=(apps[e.app]||0)+1);
    const topApps = Object.entries(apps).sort((a,b)=>b[1]-a[1]).slice(0,8).map(([app,count])=>({app,count}));

    const hours = Array.from({length:24},()=>0);
    events.filter(e=>e.type==='app_foreground').forEach(e=>{
      const h = new Date(e.ts||0).getUTCHours(); // gunakan jam UTC agar konsisten
      hours[h]++;
    });

    const day = utcDay(from);
    await db.collection('devices').doc(devId).collection('summaries').doc(day).set({
      day, notif, topApps, hours, updatedAt: admin.firestore.FieldValue.serverTimestamp()
    }, { merge: true });
  }
  return null;
});

export const purgeOldEvents = functions.pubsub.schedule('30 1 * * *').timeZone('Asia/Jakarta').onRun(async () => {
  const cutoff = Date.now() - 30*24*3600*1000;
  const devices = await db.collection('devices').get();
  for (const dev of devices.docs) {
    const devId = dev.id;
    const snap = await db.collection('devices').doc(devId).collection('events').where('ts','<', cutoff).get();
    const batch = db.batch(); snap.docs.forEach(d=>batch.delete(d.ref)); await batch.commit();
  }
  return null;
});

export const aiSummarizeDaily = functions.pubsub.schedule('10 0 * * *').timeZone('Asia/Jakarta').onRun(async () => {
  const OPENAI = process.env.OPENAI_API_KEY || functions.config().openai?.key;
  const devices = await db.collection('devices').get();
  const from = startOfPrevDayUTC(); const day = utcDay(from); const to = endOfPrevDayUTC();
  for (const dev of devices.docs) {
    const devId = dev.id;
    const sumRef = db.collection('devices').doc(devId).collection('summaries').doc(day);
    const sumSnap = await sumRef.get(); if (!sumSnap.exists) continue;
    const s = sumSnap.data() || {};

    let ai = `Ringkasan ${day}: ${s.notif||0} notifikasi. Top apps: ${(s.topApps||[]).map(x=>x.app+'('+x.count+')').join(', ')||'-'}.`;
    if (OPENAI) {
      try {
        const prompt = `Buat ringkasan harian singkat (maks 3 kalimat, bahasa Indonesia) berdasarkan data berikut:
- Total notifikasi: ${s.notif||0}
- Top apps: ${(s.topApps||[]).map(x=>x.app+'('+x.count+')').join(', ')||'-'}
- Peak hours (UTC): ${(s.hours||[]).map((v,i)=>v>0?i:null).filter(x=>x!==null).slice(0,6).join(', ') || '-'}
Fokus pada pemahaman pola pemakaian dan waktu aktif, hindari nada menghakimi.`;
        const resp = await fetch('https://api.openai.com/v1/chat/completions', {
          method:'POST', headers:{ 'Authorization':`Bearer ${OPENAI}`, 'Content-Type':'application/json' },
          body: JSON.stringify({ model:'gpt-4o-mini', messages:[{role:'user', content: prompt}], temperature: 0.3 })
        });
        const json = await resp.json();
        ai = json.choices?.[0]?.message?.content || ai;
      } catch (e) { /* ignore network errors */ }
    }
    await sumRef.set({ ai, updatedAt: admin.firestore.FieldValue.serverTimestamp() }, { merge: true });
  }
  return null;
});
