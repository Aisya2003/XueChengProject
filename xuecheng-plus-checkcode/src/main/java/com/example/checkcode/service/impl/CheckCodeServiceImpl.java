package com.example.checkcode.service.impl;

import com.example.base.exception.BusinessException;
import com.example.base.utils.EncryptUtil;
import com.example.base.utils.PhoneUtil;
import com.example.base.utils.StringUtil;
import com.example.checkcode.model.dto.CheckCodeParamsDto;
import com.example.checkcode.model.dto.CheckCodeResultDto;
import com.example.checkcode.service.ICheckCodeService;
import com.google.code.kaptcha.impl.DefaultKaptcha;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang3.RandomUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.Duration;
import java.util.Random;
import java.util.UUID;

@Service
@Slf4j
public class CheckCodeServiceImpl implements ICheckCodeService {
    private final StringRedisTemplate stringRedisTemplate;
    private final DefaultKaptcha kaptcha;

    @Autowired
    public CheckCodeServiceImpl(StringRedisTemplate stringRedisTemplate, DefaultKaptcha kaptcha) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.kaptcha = kaptcha;
    }

    @Override
    public CheckCodeResultDto generate(CheckCodeParamsDto checkCodeParamsDto) {
        //获取验证码
        String code = generateCheckCode(4);
        //获取验证key
        String key = UUID.randomUUID().toString();
        //设置验证码十分钟过期
        stringRedisTemplate.opsForValue().set(key, code, Duration.ofMinutes(10));
        //生成图片Base64
        String picBase64 = generateCheckCodePic(code);

        //封装返回对象
        CheckCodeResultDto checkCodeResultDto = new CheckCodeResultDto();
        checkCodeResultDto.setKey(key);
        checkCodeResultDto.setAliasing(picBase64);

        return checkCodeResultDto;
    }

    @Override
    public Boolean verify(String key, String code) {
        if (StringUtil.isEmpty(key) || StringUtil.isEmpty(code)) return null;
        String codeFromRedis = stringRedisTemplate.opsForValue().get(key);
        if (StringUtil.isEmpty(codeFromRedis)) return null;
        boolean result = BooleanUtils.isTrue(codeFromRedis.equalsIgnoreCase(code));
        if (result) stringRedisTemplate.delete(key);
        return result;
    }

    @Override
    public void sendPhoneCode(String phoneNumber) {
        //验证
        if (StringUtil.isEmpty(phoneNumber)) {
            throw new RuntimeException("手机号码不能为空！");
        }
        Boolean match = PhoneUtil.isMatches(phoneNumber);
        if (!match) {
            throw new RuntimeException("手机号码格式错误！");
        }

        //生成验证码
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            int item = RandomUtils.nextInt(0, 9);
            stringBuilder.append(item);
        }
        String code = stringBuilder.toString();

        //存入redis,五分钟有效期
        String key = "login:" + phoneNumber;
        stringRedisTemplate.opsForValue().set(key, code, Duration.ofMinutes(5));
        log.info("短信发送成功,{},{}", phoneNumber, code);
    }


    /**
     * 生成验证码
     *
     * @param length 验证码长度
     * @return 验证码
     */
    private String generateCheckCode(int length) {
        String str = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random random = new Random();
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < length; i++) {
            int number = random.nextInt(36);
            stringBuilder.append(str.charAt(number));
        }
        return stringBuilder.toString();
    }

    /**
     * 生成验证码图片的Base64
     *
     * @param code 验证码
     * @return Base64图片
     */
    private String generateCheckCodePic(String code) {
        String picResult = null;
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();) {
            //获取图片
            BufferedImage image = kaptcha.createImage(code);
            //写入
            ImageIO.write(image, "png", byteArrayOutputStream);
            //编码
            picResult = EncryptUtil.encodeBase64(byteArrayOutputStream.toByteArray());
        } catch (Exception e) {
            BusinessException.cast("生成验证码图片出错");
        }
        //组装编码结果
        return "data:image/png;base64," + picResult;
    }
}
