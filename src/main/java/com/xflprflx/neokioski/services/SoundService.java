package com.xflprflx.neokioski.services;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
/**
 * Serviço dedicado à manipulação de caminhos e arquivos de áudio utilizados para notificações no sistema.
 */
public class SoundService {

    private static final String SOUNDS_PATH = "classpath*:static/sounds/#alert_*";
    private static final String SOUNDS_PATH_FALLBACK = "classpath*:static/sounds/#alert_*";

    /**
     * Método ou construtor que encapsula parte da lógica de negócio e regras do serviço.
     */
    public List<String> getAvailableAlertSounds() {
        List<String> sounds = new ArrayList<>();
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        /*
         * Inicia um bloco try-catch para tratar possíveis exceções.
         */
        try {
            Resource[] res = resolver.getResources(SOUNDS_PATH);
            System.out.println("SOUND: Checking SOUNDS_PATH (" + SOUNDS_PATH + "): found " + res.length);
            addSoundsFromResources(res, sounds);
            /*
             * Avalia uma condição (if) para desvio de fluxo da lógica principal.
             */
            if (sounds.isEmpty()) {
                res = resolver.getResources(SOUNDS_PATH_FALLBACK);
                System.out.println("SOUND: Checking SOUNDS_PATH_FALLBACK (" + SOUNDS_PATH_FALLBACK + "): found " + res.length);
                addSoundsFromResources(res, sounds);
            }
        } catch (IOException e) {
            System.err.println("SOUND ERROR: Failed to scan sounds: " + e.getMessage());
            e.printStackTrace();
        }
        System.out.println("SOUND: Available sounds: " + sounds);
        /*
         * Retorna o resultado final processado pelo método.
         */
        return sounds;
    }

    /**
     * Método ou construtor que encapsula parte da lógica de negócio e regras do serviço.
     */
    private void addSoundsFromResources(Resource[] resources, List<String> sounds) {
        /*
         * Inicia um laço de repetição (for) para iterar sobre os elementos.
         */
        for (Resource resource : resources) {
            String filename = resource.getFilename();
            System.out.println("SOUND: Found file: " + filename);
            /*
             * Avalia uma condição (if) para desvio de fluxo da lógica principal.
             */
            if (filename != null && filename.startsWith("#alert_")) {
                /*
                 * Avalia uma condição (if) para desvio de fluxo da lógica principal.
                 */
                if (!sounds.contains(filename)) {
                    sounds.add(filename);
                    System.out.println("SOUND: Added to list: " + filename);
                }
            }
        }
    }
}
