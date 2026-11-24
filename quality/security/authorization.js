function sendingRequest(msg, initiator, helper) {
    var uri = msg.getRequestHeader().getURI();
    var allowed = {
        camions: 8081,
        chargements: 8082,
        bons: 8083,
        transports: 8084
    };
    if (allowed[String(uri.getHost())] === uri.getPort()
            && String(uri.getPath()).indexOf('/api/v1/') === 0) {
        var Files = Java.type('java.nio.file.Files');
        var Paths = Java.type('java.nio.file.Paths');
        var token = String(Files.readString(Paths.get('/zap/wrk/reader-token.txt'))).trim();
        msg.getRequestHeader().setHeader('Authorization', 'Bearer ' + token);
    }
}

function responseReceived(msg, initiator, helper) {
}
