const fs = require('node:fs');
const path = require('node:path');
const root = path.resolve(__dirname, '../..');
const newman = require(path.join(root, '.tools/api-quality-tools/node_modules/newman'));
const privateFolder = path.join(root, '.secrets/stage2');
const output = process.argv[2];
if (!output) throw new Error('Provide an output directory.');
fs.mkdirSync(output, {recursive:true});
const environment = JSON.parse(fs.readFileSync(path.join(privateFolder, 'postman-environment.json'),'utf8'));
const privateValues = environment.values.map(v => v.value).filter(v => typeof v === 'string' && v.length > 20);
const redact = value => {
 let data = JSON.stringify(value);
 for (const secret of privateValues) data = data.split(secret).join('[JETON_RETIRE]');
 data = data.replace(/eyJ[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+/g,'[JETON_RETIRE]');
 return JSON.parse(data);
};
newman.run({
 collection:path.join(__dirname,'Transport_DAT.postman_collection.json'), environment,
 reporters:['cli','json','junit'],
 reporter:{json:{export:path.join(privateFolder,'newman-original.json')},
           junit:{export:path.join(output,'newman-junit.xml')}},
 timeoutRequest:20000, bail:true
}, (error, summary) => {
 if (error || !summary) { console.error(error?.message || 'Newman did not return results');process.exitCode=2;return; }
 const executions=summary.run.executions.map(e=>({
  nom:e.item.name,methode:e.request.method,url:e.request.url.toString(),
  corpsRequete:e.request.body?.raw || null,
  statutHTTP:e.response?.code || null,dureeMs:e.response?.responseTime || null,
  corpsReponse:e.response?e.response.stream.toString('utf8'):null,
  assertions:(e.assertions||[]).map(a=>({nom:a.assertion,reussie:!a.error,erreur:a.error?.message||null}))
 }));
 const result=redact({outil:'Newman 6.2.3 / collection Postman v2.1',date:new Date().toISOString(),
  reussi:!summary.run.failures.length,statistiques:summary.run.stats,executions,
  echecs:summary.run.failures.map(f=>({nom:f.error?.test||f.error?.name,message:f.error?.message,source:f.source?.name})),
  contexte:{base:'PostgreSQL 18 local isolé',servicesReels:true,keycloakReel:true,mocksMetier:false,frontendUtilise:false},
  confidentialite:'Valeurs des jetons retirées des exports partageables.'});
 fs.writeFileSync(path.join(output,'resultats.json'),JSON.stringify(result,null,2));
 fs.writeFileSync(path.join(output,'newman-original-expurge.json'),JSON.stringify(redact(summary),null,2));
 const sources = {truckId:'Créer le camion de référence',loadingId:'Créer un brouillon complet',
                  bonId:'Émettre le bon à partir du chargement validé',transportId:'Planifier et réserver le camion'};
 const fixtures=Object.fromEntries(Object.entries(sources).map(([key,name])=>{
  const execution=executions.find(e=>e.nom===name);
  return [key,execution?.corpsReponse?JSON.parse(execution.corpsReponse).id:null];
 }));
 if (result.reussi) fs.writeFileSync(path.join(privateFolder,'fixtures.json'),JSON.stringify(fixtures,null,2));
 console.log('Résultat :',executions.length,'requêtes ;',summary.run.stats.assertions.total,'assertions ;',summary.run.failures.length,'échecs.');
 process.exitCode=result.reussi?0:1;
});
