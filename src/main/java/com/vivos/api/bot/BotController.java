package com.vivos.api.bot;

import com.vivos.api.bot.BotDtos.BotClientResponse;
import com.vivos.api.bot.BotDtos.BotDebtResponse;
import com.vivos.api.bot.BotDtos.BotRegisterRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * API para el bot de WhatsApp (darmoz-whatsapp). Autenticada con la API key
 * ({@code X-Api-Key}); la tienda y la clienta salen de los headers de WhatsApp.
 */
@RestController
@RequestMapping("/bot")
public class BotController {

    private final BotContextResolver contextResolver;
    private final BotService botService;

    BotController(BotContextResolver contextResolver, BotService botService) {
        this.contextResolver = contextResolver;
        this.botService = botService;
    }

    /** 200 si quien escribe es clienta de la tienda, 404 si no. */
    @GetMapping("/me")
    BotClientResponse me(@RequestHeader(value = BotContextResolver.HEADER_WABA_ID, required = false) String wabaId,
                         @RequestHeader(value = BotContextResolver.HEADER_FROM, required = false) String from) {
        return botService.me(contextResolver.resolve(wabaId, from));
    }

    /** Cuanto debe del ultimo vivo en que compro. */
    @GetMapping("/debt")
    BotDebtResponse debt(@RequestHeader(value = BotContextResolver.HEADER_WABA_ID, required = false) String wabaId,
                         @RequestHeader(value = BotContextResolver.HEADER_FROM, required = false) String from) {
        return botService.debt(contextResolver.resolve(wabaId, from));
    }

    /** Auto-registro como clienta de la tienda. 409 si ya lo es. */
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    BotClientResponse register(@RequestHeader(value = BotContextResolver.HEADER_WABA_ID, required = false) String wabaId,
                               @RequestHeader(value = BotContextResolver.HEADER_FROM, required = false) String from,
                               @Valid @RequestBody BotRegisterRequest request) {
        return botService.register(contextResolver.resolve(wabaId, from), request);
    }
}
