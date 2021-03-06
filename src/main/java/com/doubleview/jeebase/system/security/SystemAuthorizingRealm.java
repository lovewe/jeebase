package com.doubleview.jeebase.system.security;

import com.doubleview.jeebase.support.config.Constant;
import com.doubleview.jeebase.support.render.CaptchaRender;
import com.doubleview.jeebase.support.utils.EncodeUtils;
import com.doubleview.jeebase.system.model.User;
import com.doubleview.jeebase.system.service.UserService;
import com.doubleview.jeebase.system.utils.ShiroUtils;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authc.credential.HashedCredentialsMatcher;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.session.Session;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.util.ByteSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;

/**
 * 系统安全认证类
 */
public class SystemAuthorizingRealm extends AuthorizingRealm {

    private Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private UserService userService;

    /**
     * 设定密码校验的Hash算法与迭代次数(对象构造完自动调用)
     */
    @PostConstruct
    public void initCredentialsMatcher() {
        HashedCredentialsMatcher matcher = new HashedCredentialsMatcher(ShiroUtils.HASH_ALGORITHM);
        matcher.setHashIterations(ShiroUtils.HASH_INTERATIONS);
        setCredentialsMatcher(matcher);
    }

    /**
     * 获取认证信息
     *
     * @param token
     * @return
     * @throws AuthenticationException
     */
    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token){
        SystemToken systemToken = (SystemToken) token;
        logger.debug("user {} login submit",systemToken.getUsername());
        Session session = SecurityUtils.getSubject().getSession();
        if (!CaptchaRender.validate(session, systemToken.getCaptcha())) {
            throw new AuthenticationException("error:验证码有误, 请重试.");
        }

        User user = userService.getByLoginName(systemToken.getUsername());
        if (user != null) {
            if (user.getLoginFlag().equals(Constant.NO))throw new AuthenticationException("error:您被禁止登录");

            String salt = user.getPassword().substring(0, ShiroUtils.SALT_SIZE*2);
            ByteSource saltSource = ByteSource.Util.bytes(EncodeUtils.decodeHex(salt));
            return new SimpleAuthenticationInfo(user, user.getPassword().substring(16),saltSource, this.getName());
        } else {
            throw new AuthenticationException("error:用户名不存在");
        }

    }

    /**
     * 获取授权信息
     *
     * @param principals
     * @return
     */
    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
        AuthorizationInfo authorizationInfo = new SimpleAuthorizationInfo();
        return authorizationInfo;
    }
}
