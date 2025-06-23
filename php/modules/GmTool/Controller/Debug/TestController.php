<?php

namespace App\Modules\GmTool\Controller\Debug;

use App\Modules\GmTool\Foundation\Controller;

class TestController extends Controller
{
    public function indexAction(): void
    {
        // 基本的な変数
        $intVar = 42;
        $floatVar = 3.14;
        $this->setVar('sum', $intVar + $floatVar . 'aaa');
        $this->setVar('quest', null);
    }
}
