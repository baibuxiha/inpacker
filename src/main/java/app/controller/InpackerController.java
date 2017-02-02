package app.controller;

import app.core.InpackerService;
import app.core.model.PackSettings;
import app.core.model.User;
import app.dto.MessageResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.File;

import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

@Controller
public class InpackerController {

    private final InpackerService service;

    @Autowired
    public InpackerController(InpackerService inpackerService) {
        service = inpackerService;
    }

    @RequestMapping(value = "api/user/{username:.+}", method = GET)
    public ResponseEntity<?> getUser(@PathVariable String username) {
        final User user = service.getUser(username);
        if (user == null) {
            return ResponseEntity.status(404).body(new MessageResponse("Not Found"));
        } else {
            return ResponseEntity.ok(user);
        }
    }

    @RequestMapping(value = "api/pack/{username:.+}", method = POST)
    public ResponseEntity<MessageResponse> createPack(
            @PathVariable String username,
            @RequestBody PackSettings packSettings) {
        service.createPack(username, packSettings);
        return ResponseEntity.ok(new MessageResponse(String.format("/packs/%s.zip", username)));
    }

    @RequestMapping(value = "packs/{username:.+}.zip", method = GET, produces = "application/zip")
    @ResponseBody
    public ResponseEntity<FileSystemResource> downloadPack(@PathVariable String username) {
        final File packFile = service.getPackFile(username);
        if (packFile == null)
            return ResponseEntity.status(404).body(null);
        else
            return ResponseEntity.ok(new FileSystemResource(packFile));
    }
}