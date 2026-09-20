from pathlib import Path
import json
root=Path(__file__).parent
project={"schema":"trace.studio/3","name":"Trace Studio · Native sample","brand":{"name":"Trace","account":"Trace 생활통장","logo":None,"logoWidth":26,"logoHeight":26,"showName":True},"tokens":{"primary":"#D93B25","background":"#FBFAF7","surface":"#FFFFFF","ink":"#20211F","muted":"#696B64","border":"#E2E1DA","radius":14,"padding":24,"gap":18,"titleSize":28,"bodySize":16,"amountSize":38,"buttonHeight":56,"buttonRadius":12,"font":"system","motion":180,"easing":"ease-out","buttonStyle":"solid"},"senior":{"enabled":False,"scale":1.2,"touch":64,"reducedMotion":True},"nav":["home","assets","recipient","safety","settings"],"screens":[]}
def node(type,text='',props=None,children=None):
 n={"id":"", "type":type,"name":type,"text":text,"visible":True,"props":props or {}}
 if children is not None:n['children']=children
 return n
def page(id,name,blocks,footer=None,group='은행'):
 count=0
 def stamp(n):
  nonlocal count
  count+=1;n['id']='n'+id+str(count)
  for child in n.get('children',[]):stamp(child)
 for n in blocks:stamp(n)
 project['screens'].append(dict(id=id,name=name,group=group,blocks=blocks,footer=footer))
def footer(label,action,secondary=None):return dict(primary=label,action=action,secondary=secondary,secondaryAction='cancel' if secondary else None)
page('home','홈',[node('balance'),node('quick'),node('history','최근 거래'),node('safety')])
page('assets','자산',[node('heading','내 자산'),node('balance','총 보유 자산',{'mode':'total'}),node('assets')])
page('recipient','받는 분',[node('heading','누구에게 보낼까요?'),node('recipients')])
page('amount','금액 입력',[node('heading','얼마를 보낼까요?'),node('amount')],footer('다음','review'))
page('review','최종 확인',[node('heading','이서연님에게',{'bind':'recipient'}),node('transaction','보낼 금액',{'compact':True}),node('review')],footer('인증하고 보내기','authorize'))
page('success','송금 완료',[node('receipt'),node('review')],footer('확인','home'))
for id,name,title,extra,action,label in [('hold','송금 보류','잠깐,\n확인하고 보내볼까요?','reasons','guide','안전하게 확인하기'),('verify','상환 경로 확인','대출을 갚는\n돈이 맞나요?','reasons','resolve','공식 상환 경로 확인'),('unknown','경로 확인 불가','확인할 수 없으면,\n보내지 않습니다.','notice','resolve','공식 경로 다시 확인'),('warn','보내기 전 확인','보내기 전에\n하나만 확인해 주세요.','notice','warnCheck','다른 경로로 확인')]:
 page(id,name,[node('brand'),node('heading',title),node('reassurance'),node('transaction'),node(extra,'상대가 제공한 번호나 링크가 아닌 공식 경로에서 확인하세요.')],footer(label,action,'송금 취소'),'보호')
page('route','확인된 상환처',[node('brand'),node('heading','상환할 곳을\n확인했어요.'),node('reassurance'),node('route')],footer('새 송금 내역 확인','useRoute'))
page('guide','안전하게 확인',[node('brand'),node('heading','상대가 준 번호나\n링크는 쓰지 마세요.'),node('text','평소 쓰던 은행 앱에서 직접 확인하세요.'),node('reassurance')],footer('다음 확인 단계','guideNext','송금 취소'))
page('timeline','위험 신호의 흐름',[node('heading','짧은 시간 안에\n이어진 위험 신호'),node('timeline')])
page('safety','안전 센터',[node('heading','평소에는 조용하게.\n위험할 땐 분명하게.'),node('safety'),node('menu','',{'items':[['위험 신호의 흐름','timeline'],['시연 센터','demo']]})])
page('demo','시연 센터',[node('brand'),node('heading','같은 송금,\n다른 상황.'),node('cases'),node('modes')])
page('settings','전체',[node('heading','전체'),node('menu','',{'items':[['시연 센터','demo'],['노약자 모드','accessibility'],['개인정보','privacy'],['고객지원','support']]})])
page('accessibility','화면과 안내',[node('heading','편하게 보세요.'),node('modes')])
page('history','거래 내역',[node('heading','거래 내역'),node('history','전체 내역')])
page('account','계좌 상세',[node('balance'),node('quick'),node('history','최근 거래')])
page('privacy','개인정보',[node('heading','지키기 위해,\n가져가지 않습니다.'),node('text','원문은 전송하지 않는 오프라인 시연입니다.')])
page('support','고객지원',[node('heading','무엇을 도와드릴까요?'),node('menu','',{'items':[['송금이 보류되었어요','guide'],['개인정보 안내','privacy']]})])
frame=node('frame',props={'layout':'column','padding':20,'gap':16,'fill':'#F2F0E9','radius':20,'widthMode':'fill'},children=[node('richtext','도형과 곡선도 네이티브로.',{'fontSize':24,'weight':700}),node('path',props={'position':'flow','width':270,'height':100,'fill':'none','stroke':'#D93B25','strokeWidth':4,'anchors':[{'x':0,'y':90,'outX':35,'outY':-20},{'x':100,'y':20,'inX':65,'inY':110}],'motionType':'fade','motion':{'duration':180,'delay':0}}),node('richtext','같은 디자인 문서의 베지어 경로와 프레임을 Compose Canvas로 그립니다.',{'fontSize':16,'weight':400})])
page('vectors','벡터 렌더링',[frame,node('rectangle',props={'position':'flow','width':220,'height':86,'fill':'#ECE7DD','stroke':'#958575','strokeWidth':1,'corners':[24,4,24,4]}),node('richtext','노약자 모드에서도 보호 정책은 유지됩니다.',{'fontSize':16})],group='Studio')
(root/'design.json').write_text(json.dumps(project,ensure_ascii=False,indent=2))
print('Native sample design:',len(project['screens']),'screens')
