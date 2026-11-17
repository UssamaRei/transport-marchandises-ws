package ma.transport.chargements.proxy;

import ma.transport.chargements.model.Camion;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "camions", url = "${camions.service.url}")
public interface CamionsProxy {

    @GetMapping("/api/v1/camions/{id}")
    Camion findCamionById(@PathVariable("id") String id);
}
