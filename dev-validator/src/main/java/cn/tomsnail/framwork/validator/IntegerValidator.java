package cn.tomsnail.framwork.validator;

import java.util.Map;

import org.apache.commons.lang.StringUtils;

import cn.tomsnail.framwork.validator.exception.ParamValidatorException;


/**
 *        Integer值验证
 * @author yangsong
 * @version 0.0.1
 * @status 正常
 * @date 2016年9月21日 下午4:05:03
 * @see 
 */
public class IntegerValidator extends AValidator<Integer> {

	@Override
	public Integer getValidatorValue(Map<String, Object> valueMap, String key,String errorMsg)
			throws ParamValidatorException {
		if(valueMap==null||key==null){
			throw new ParamValidatorException(this.getValidFaildMsg(errorMsg, "Map值或者Key值空异常"));
		}
		if(!doCustValidators(valueMap)){
			throw new ParamValidatorException(this.getValidFaildMsg(errorMsg, "custvalidator error"));
		}
		String value = (String) valueMap.get(key);
		Integer v = null;
		try{
			v = Integer.valueOf(value);
		}catch(NumberFormatException exception){
			if(value==null||value.equals("")){
			}else{
				throw new ParamValidatorException(this.getValidFaildMsg(errorMsg, key+"的值不是Integer类型"+value));
			}
		}
		super.rule.validator(key,v,errorMsg);
		return v;
	}

	@Override
	public Integer getValidatorValue(Map<String, Object> valueMap, String key)
			throws ParamValidatorException {
		return this.getValidatorValue(valueMap, key, "");
	}

	
	
	

	

}
