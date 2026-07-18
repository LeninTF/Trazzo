package trazzo.back.saasglobal.application.port.in;

import trazzo.back.saasglobal.application.dto.command.ShopCheckoutCommand;
import trazzo.back.saasglobal.application.dto.result.ShopCheckoutResult;

public interface ShopCheckoutUseCase {
    ShopCheckoutResult checkout(ShopCheckoutCommand command);
}
