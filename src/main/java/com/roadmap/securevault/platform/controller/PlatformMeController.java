package com.roadmap.securevault.platform.controller;

import com.roadmap.securevault.common.annotation.ApiVersion;
import com.roadmap.securevault.common.controller.BaseMeController;
import com.roadmap.securevault.common.dto.MeResponse;
import com.roadmap.securevault.platform.entity.PlatformUser;
import com.roadmap.securevault.platform.mapper.PlatformUserMapper;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/platform")
@ApiVersion("v1")
public class PlatformMeController extends BaseMeController<PlatformUser, MeResponse> {

    @Override
    protected MeResponse toMeResponse(PlatformUser user) {
        return PlatformUserMapper.toMeResponse(user);
    }
}